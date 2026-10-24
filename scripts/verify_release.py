#!/usr/bin/env python3
"""Verify signed artifacts and ELF segment alignment without printing signing secrets."""
from pathlib import Path
import hashlib
import json
import struct
import subprocess
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APK = ROOT / 'app/build/outputs/apk/release/app-release.apk'
AAB = ROOT / 'app/build/outputs/bundle/release/app-release.aab'
sdk_properties = dict(line.split('=', 1) for line in (ROOT / 'local.properties').read_text().splitlines()
                      if '=' in line and not line.startswith('#'))
sdk = Path(sdk_properties['sdk.dir'])
versions = sorted((sdk / 'build-tools').iterdir(), key=lambda p: tuple(int(n) for n in p.name.split('.') if n.isdigit()))
build_tools = versions[-1]
subprocess.run([str(build_tools / 'apksigner'), 'verify', '--verbose', str(APK)], check=True)
subprocess.run([str(build_tools / 'zipalign'), '-c', '-P', '16', '4', str(APK)], check=True)
result = subprocess.run(['jarsigner', '-verify', str(AAB)], capture_output=True, text=True, check=True)
if 'jar verified.' not in result.stdout:
    raise SystemExit('App Bundle JAR signature could not be verified')
print('AAB signature verified (local self-signed upload certificate).')
manifest = ET.parse(ROOT / 'app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
application = manifest.find('application')
assert application.get(android + 'debuggable') != 'true', 'Release must not be debuggable'
assert application.get(android + 'allowBackup') == 'false', 'Private drafts must be excluded from backup'
for feature in manifest.findall('uses-feature'):
    assert feature.get(android + 'required', 'true') != 'true', 'Review required device hardware before release'
with zipfile.ZipFile(APK) as archive:
    assert 'assets/privacy_policy.txt' in archive.namelist(), 'Missing in-app privacy policy'
with zipfile.ZipFile(AAB) as archive:
    assert 'base/assets/privacy_policy.txt' in archive.namelist(), 'Missing in-app privacy policy in bundle'
print('Release manifest and packaged privacy policy verified.')
report = {'artifacts': [], 'native_libraries': []}
for artifact in (APK, AAB):
    report['artifacts'].append({'file': str(artifact.relative_to(ROOT)), 'bytes': artifact.stat().st_size,
                                'sha256': hashlib.sha256(artifact.read_bytes()).hexdigest()})
    with zipfile.ZipFile(artifact) as archive:
        for entry in archive.namelist():
            if not entry.endswith('.so'):
                continue
            data = archive.read(entry)
            assert data[:4] == b'\x7fELF' and data[4] == 2, f'Expected 64-bit ELF: {entry}'
            endian = '<' if data[5] == 1 else '>'
            offset = struct.unpack_from(endian + 'Q', data, 32)[0]
            size, count = struct.unpack_from(endian + 'HH', data, 54)
            alignments = []
            for i in range(count):
                header = struct.unpack_from(endian + 'IIQQQQQQ', data, offset + i * size)
                if header[0] == 1:  # PT_LOAD
                    alignment = header[7]
                    assert alignment >= 16384, f'Not 16 KB aligned: {entry}'
                    assert (header[2] - header[3]) % 16384 == 0, f'Incompatible segment offset: {entry}'
                    alignments.append(alignment)
            assert alignments, f'No loadable segments: {entry}'
            report['native_libraries'].append({'artifact': artifact.name, 'library': entry, 'load_alignments': alignments})
            print(f'16 KB ELF alignment verified: {artifact.name}: {entry}')
output = ROOT / 'release-artifacts'
output.mkdir(exist_ok=True)
(output / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
print('Verification report: release-artifacts/verification.json')
