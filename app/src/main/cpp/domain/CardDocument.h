#ifndef WISHFEST_CARDDOCUMENT_H
#define WISHFEST_CARDDOCUMENT_H

#include <string>
#include <vector>
#include <memory>
#include <utility>
#include "Layer.h"

namespace wishfest {

struct CanvasConfig {
    int width = 1080;
    int height = 1080;
    std::string backgroundColor = "#FFFFFF";
    std::string backgroundPhotoPath;
};

class CardDocument {
public:
    CardDocument(std::string id, std::string templateId, CanvasConfig canvas)
        : id_(std::move(id)), templateId_(std::move(templateId)), canvas_(std::move(canvas)) {}

    [[nodiscard]] const std::string& getId() const { return id_; }
    [[nodiscard]] const std::string& getTemplateId() const { return templateId_; }

    [[nodiscard]] const CanvasConfig& getCanvas() const { return canvas_; }
    void setCanvas(const CanvasConfig& canvas) { canvas_ = canvas; }

    [[nodiscard]] const std::vector<std::shared_ptr<Layer>>& getLayers() const { return layers_; }

    void addLayer(std::shared_ptr<Layer> layer);
    bool removeLayer(const std::string& layerId);
    [[nodiscard]] std::shared_ptr<Layer> findLayer(const std::string& layerId) const;
    void clearLayers();
    void moveLayerUp(const std::string& layerId);
    void moveLayerDown(const std::string& layerId);

private:
    std::string id_;
    std::string templateId_;
    CanvasConfig canvas_;
    std::vector<std::shared_ptr<Layer>> layers_;
};

} // namespace wishfest

#endif // WISHFEST_CARDDOCUMENT_H
