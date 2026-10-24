#include "CardDocument.h"
#include <algorithm>

namespace wishfest {

void CardDocument::addLayer(std::shared_ptr<Layer> layer) {
    if (layer) {
        layers_.push_back(std::move(layer));
    }
}

bool CardDocument::removeLayer(const std::string& layerId) {
    for (auto it = layers_.begin(); it != layers_.end(); ++it) {
        if ((*it)->getId() == layerId) {
            layers_.erase(it);
            return true;
        }
    }
    return false;
}

std::shared_ptr<Layer> CardDocument::findLayer(const std::string& layerId) const {
    for (const auto& layer : layers_) {
        if (layer->getId() == layerId) return layer;
    }
    return nullptr;
}

void CardDocument::clearLayers() {
    layers_.clear();
}

void CardDocument::moveLayerUp(const std::string& layerId) {
    for (size_t i = 0; i + 1 < layers_.size(); ++i) {
        if (layers_[i]->getId() == layerId) {
            std::swap(layers_[i], layers_[i + 1]);
            break;
        }
    }
}

void CardDocument::moveLayerDown(const std::string& layerId) {
    for (size_t i = 1; i < layers_.size(); ++i) {
        if (layers_[i]->getId() == layerId) {
            std::swap(layers_[i], layers_[i - 1]);
            break;
        }
    }
}

} // namespace wishfest
