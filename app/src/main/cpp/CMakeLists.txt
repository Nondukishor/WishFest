cmake_minimum_required(VERSION 3.22.1)

project("wishfest_native")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Configure 16 KB page alignment compatibility for NDK builds
add_link_options("-Wl,-z,max-page-size=16384")

include_directories(
    domain
    serialization
    bridge
)

add_library(
    wishfest_native
    SHARED
    domain/CardDocument.cpp
    serialization/Serializer.cpp
    bridge/wishfest_jni.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    wishfest_native
    ${log-lib}
)
