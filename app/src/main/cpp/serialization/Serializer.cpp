#include "Serializer.h"
#include <sstream>
#include <iomanip>
#include <regex>

namespace wishfest {

std::string Serializer::escapeJsonString(const std::string& input) {
    std::ostringstream ss;
    for (char c : input) {
        switch (c) {
            case '"':  ss << "\\\""; break;
            case '\\': ss << "\\\\"; break;
            case '\b': ss << "\\b";  break;
            case '\f': ss << "\\f";  break;
            case '\n': ss << "\\n";  break;
            case '\r': ss << "\\r";  break;
            case '\t': ss << "\\t";  break;
            default:
                if ('\x00' <= c && c <= '\x1f') {
                    ss << "\\u" << std::hex << std::setw(4) << std::setfill('0') << static_cast<int>(c);
                } else {
                    ss << c;
                }
                break;
        }
    }
    return ss.str();
}

std::string Serializer::serialize(const CardDocument& doc) {
    std::ostringstream ss;
    ss << "{\n";
    ss << "  \"schemaVersion\": 1,\n";
    ss << R"(  "id": ")" << escapeJsonString(doc.getId()) << "\",\n";
    ss << R"(  "templateId": ")" << escapeJsonString(doc.getTemplateId()) << "\",\n";

    const auto& canvas = doc.getCanvas();
    ss << "  \"canvas\": {\n";
    ss << "    \"width\": " << canvas.width << ",\n";
    ss << "    \"height\": " << canvas.height << ",\n";
    ss << R"(    "backgroundColor": ")" << escapeJsonString(canvas.backgroundColor) << "\",\n";
    ss << R"(    "backgroundPhotoPath": ")" << escapeJsonString(canvas.backgroundPhotoPath) << "\"\n";
    ss << "  },\n";

    ss << "  \"layers\": [\n";
    const auto& layers = doc.getLayers();
    for (size_t i = 0; i < layers.size(); ++i) {
        const auto& layer = layers[i];
        ss << "    {\n";
        ss << R"(      "id": ")" << escapeJsonString(layer->getId()) << "\",\n";

        if (layer->getType() == LayerType::Text) {
            ss << "      \"type\": \"text\",\n";
        } else {
            ss << "      \"type\": \"image\",\n";
        }

        const auto& t = layer->getTransform();
        ss << R"(      "transform": {"x": )" << t.x << ", \"y\": " << t.y
           << ", \"width\": " << t.width << ", \"height\": " << t.height
           << ", \"rotation\": " << t.rotation << "},\n";
        ss << "      \"opacity\": " << layer->getOpacity() << ",\n";
        ss << "      \"visible\": " << (layer->isVisible() ? "true" : "false") << ",\n";
        ss << "      \"locked\": " << (layer->isLocked() ? "true" : "false");

        if (layer->getType() == LayerType::Text) {
            auto textLayer = std::dynamic_pointer_cast<TextLayer>(layer);
            if (textLayer) {
                ss << ",\n";
                ss << R"(      "text": ")" << escapeJsonString(textLayer->getText()) << "\",\n";
                ss << R"(      "fontId": ")" << escapeJsonString(textLayer->getFontId()) << "\",\n";
                ss << "      \"fontSize\": " << textLayer->getFontSize() << ",\n";
                ss << R"(      "color": ")" << escapeJsonString(textLayer->getColor()) << "\",\n";
                ss << R"(      "alignment": ")" << escapeJsonString(textLayer->getAlignment()) << "\"\n";
            } else {
                ss << "\n";
            }
        } else if (layer->getType() == LayerType::Image) {
            auto imageLayer = std::dynamic_pointer_cast<ImageLayer>(layer);
            if (imageLayer) {
                ss << ",\n";
                ss << R"(      "assetPath": ")" << escapeJsonString(imageLayer->getAssetPath()) << "\"\n";
            } else {
                ss << "\n";
            }
        }
        ss << "    }" << (i + 1 < layers.size() ? "," : "") << "\n";
    }
    ss << "  ]\n";
    ss << "}\n";

    return ss.str();
}

static std::string extractStringField(const std::string& json, const std::string& fieldName, const std::string& defaultVal = "") {
    std::regex re("\"" + fieldName + "\":\\s*\"([^\"]*)\"");
    std::smatch match;
    if (std::regex_search(json, match, re) && match.size() > 1) {
        return match.str(1);
    }
    return defaultVal;
}

static float extractFloatField(const std::string& json, const std::string& fieldName, float defaultVal = 0.0f) {
    std::regex re("\"" + fieldName + R"(":\s*([-+]?[0-9]*\.?[0-9]+))");
    std::smatch match;
    if (std::regex_search(json, match, re) && match.size() > 1) {
        return std::stof(match.str(1));
    }
    return defaultVal;
}

static int extractIntField(const std::string& json, const std::string& fieldName, int defaultVal = 0) {
    std::regex re("\"" + fieldName + "\":\\s*([0-9]+)");
    std::smatch match;
    if (std::regex_search(json, match, re) && match.size() > 1) {
        return std::stoi(match.str(1));
    }
    return defaultVal;
}

std::shared_ptr<CardDocument> Serializer::deserialize(const std::string& jsonStr) {
    std::string docId = extractStringField(jsonStr, "id", "doc-1");
    std::string templateId = extractStringField(jsonStr, "templateId", "template-1");

    CanvasConfig canvas;
    canvas.width = extractIntField(jsonStr, "width", 1080);
    canvas.height = extractIntField(jsonStr, "height", 1080);
    canvas.backgroundColor = extractStringField(jsonStr, "backgroundColor", "#FFFFFF");
    canvas.backgroundPhotoPath = extractStringField(jsonStr, "backgroundPhotoPath", "");

    auto doc = std::make_shared<CardDocument>(docId, templateId, canvas);

    // Simple parser for layers array blocks
    std::regex layerBlockRegex(R"(\{[\s\S]*?\"id\":\s*\"([^\"]*)\"[\s\S]*?\})");
    auto words_begin = std::sregex_iterator(jsonStr.begin(), jsonStr.end(), layerBlockRegex);
    auto words_end = std::sregex_iterator();

    for (std::sregex_iterator i = words_begin; i != words_end; ++i) {
        const std::smatch& match = *i;
        std::string block = match.str(0);

        std::string layerId = extractStringField(block, "id");
        if (layerId.empty() || layerId == docId) continue;

        std::string type = extractStringField(block, "type");
        Transform t;
        t.x = extractFloatField(block, "x", 0.0f);
        t.y = extractFloatField(block, "y", 0.0f);
        t.width = extractFloatField(block, "width", 200.0f);
        t.height = extractFloatField(block, "height", 100.0f);
        t.rotation = extractFloatField(block, "rotation", 0.0f);

        if (type == "text") {
            std::string text = extractStringField(block, "text");
            std::string fontId = extractStringField(block, "fontId", "default");
            float fontSize = extractFloatField(block, "fontSize", 48.0f);
            std::string color = extractStringField(block, "color", "#000000");
            std::string alignment = extractStringField(block, "alignment", "center");

            auto textLayer = std::make_shared<TextLayer>(layerId, t, text, fontId, fontSize, color, alignment);
            doc->addLayer(textLayer);
        } else if (type == "image") {
            std::string assetPath = extractStringField(block, "assetPath");
            auto imgLayer = std::make_shared<ImageLayer>(layerId, t, assetPath);
            doc->addLayer(imgLayer);
        }
    }

    return doc;
}

} // namespace wishfest
