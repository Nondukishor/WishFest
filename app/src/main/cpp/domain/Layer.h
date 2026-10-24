#ifndef WISHFEST_LAYER_H
#define WISHFEST_LAYER_H

#include <string>
#include <utility>

namespace wishfest {

enum class LayerType {
    Text,
    Image
};

struct Transform {
    float x = 0.0f;
    float y = 0.0f;
    float width = 200.0f;
    float height = 100.0f;
    float rotation = 0.0f;
};

class Layer {
public:
    Layer(std::string id, LayerType type, Transform transform)
        : id_(std::move(id)), type_(type), transform_(transform) {}

    virtual ~Layer() = default;

    [[nodiscard]] const std::string& getId() const { return id_; }
    [[nodiscard]] LayerType getType() const { return type_; }

    [[nodiscard]] const Transform& getTransform() const { return transform_; }
    void setTransform(const Transform& transform) { transform_ = transform; }

    [[nodiscard]] float getOpacity() const { return opacity_; }
    void setOpacity(float opacity) { opacity_ = opacity; }

    [[nodiscard]] bool isVisible() const { return visible_; }
    void setVisible(bool visible) { visible_ = visible; }

    bool isLocked() const { return locked_; }
    void setLocked(bool locked) { locked_ = locked; }

private:
    std::string id_;
    LayerType type_;
    Transform transform_;
    float opacity_ = 1.0f;
    bool visible_ = true;
    bool locked_ = false;
};

class TextLayer : public Layer {
public:
    TextLayer(std::string id, Transform transform, std::string text,
              std::string fontId, float fontSize, std::string color, std::string alignment)
        : Layer(std::move(id), LayerType::Text, transform),
          text_(std::move(text)),
          fontId_(std::move(fontId)),
          fontSize_(fontSize),
          color_(std::move(color)),
          alignment_(std::move(alignment)) {}

    [[nodiscard]] const std::string& getText() const { return text_; }
    void setText(std::string text) { text_ = std::move(text); }

    [[nodiscard]] const std::string& getFontId() const { return fontId_; }
    void setFontId(std::string fontId) { fontId_ = std::move(fontId); }

    [[nodiscard]] float getFontSize() const { return fontSize_; }
    void setFontSize(float fontSize) { fontSize_ = fontSize; }

    [[nodiscard]] const std::string& getColor() const { return color_; }
    void setColor(std::string color) { color_ = std::move(color); }

    [[nodiscard]] const std::string& getAlignment() const { return alignment_; }
    void setAlignment(std::string alignment) { alignment_ = std::move(alignment); }

private:
    std::string text_;
    std::string fontId_;
    float fontSize_;
    std::string color_;
    std::string alignment_;
};

class ImageLayer : public Layer {
public:
    ImageLayer(std::string id, Transform transform, std::string assetPath)
        : Layer(std::move(id), LayerType::Image, transform),
          assetPath_(std::move(assetPath)) {}

    [[nodiscard]] const std::string& getAssetPath() const { return assetPath_; }
    void setAssetPath(std::string path) { assetPath_ = std::move(path); }

private:
    std::string assetPath_;
};

} // namespace wishfest

#endif // WISHFEST_LAYER_H
