#ifndef WISHFEST_COMMANDS_H
#define WISHFEST_COMMANDS_H

#include <memory>
#include <vector>
#include <utility>
#include "CardDocument.h"

namespace wishfest {

class Command {
public:
    virtual ~Command() = default;
    virtual void execute(CardDocument& doc) = 0;
    virtual void undo(CardDocument& doc) = 0;
};

class CommandManager {
public:
    void executeCommand(CardDocument& doc, std::unique_ptr<Command> cmd) {
        cmd->execute(doc);
        undoStack_.push_back(std::move(cmd));
        redoStack_.clear();
    }

    bool canUndo() const { return !undoStack_.empty(); }
    bool canRedo() const { return !redoStack_.empty(); }

    void undo(CardDocument& doc) {
        if (undoStack_.empty()) return;
        auto cmd = std::move(undoStack_.back());
        undoStack_.pop_back();
        cmd->undo(doc);
        redoStack_.push_back(std::move(cmd));
    }

    void redo(CardDocument& doc) {
        if (redoStack_.empty()) return;
        auto cmd = std::move(redoStack_.back());
        redoStack_.pop_back();
        cmd->execute(doc);
        undoStack_.push_back(std::move(cmd));
    }

    void clear() {
        undoStack_.clear();
        redoStack_.clear();
    }

private:
    std::vector<std::unique_ptr<Command>> undoStack_;
    std::vector<std::unique_ptr<Command>> redoStack_;
};

class AddLayerCommand : public Command {
public:
    explicit AddLayerCommand(std::shared_ptr<Layer> layer)
        : layer_(std::move(layer)) {}

    void execute(CardDocument& doc) override {
        doc.addLayer(layer_);
    }

    void undo(CardDocument& doc) override {
        doc.removeLayer(layer_->getId());
    }

private:
    std::shared_ptr<Layer> layer_;
};

class RemoveLayerCommand : public Command {
public:
    explicit RemoveLayerCommand(std::shared_ptr<Layer> layer)
        : layer_(std::move(layer)) {}

    void execute(CardDocument& doc) override {
        doc.removeLayer(layer_->getId());
    }

    void undo(CardDocument& doc) override {
        doc.addLayer(layer_);
    }

private:
    std::shared_ptr<Layer> layer_;
};

class UpdateTransformCommand : public Command {
public:
    UpdateTransformCommand(std::string layerId, Transform oldT, Transform newT)
        : layerId_(std::move(layerId)), oldTransform_(oldT), newTransform_(newT) {}

    void execute(CardDocument& doc) override {
        if (auto layer = doc.findLayer(layerId_)) {
            layer->setTransform(newTransform_);
        }
    }

    void undo(CardDocument& doc) override {
        if (auto layer = doc.findLayer(layerId_)) {
            layer->setTransform(oldTransform_);
        }
    }

private:
    std::string layerId_;
    Transform oldTransform_;
    Transform newTransform_;
};

} // namespace wishfest

#endif // WISHFEST_COMMANDS_H
