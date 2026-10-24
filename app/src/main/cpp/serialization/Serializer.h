#ifndef WISHFEST_SERIALIZER_H
#define WISHFEST_SERIALIZER_H

#include <string>
#include <memory>
#include "CardDocument.h"

namespace wishfest {

class Serializer {
public:
    static std::string serialize(const CardDocument& doc);
    static std::shared_ptr<CardDocument> deserialize(const std::string& jsonStr);

private:
    static std::string escapeJsonString(const std::string& input);
};

} // namespace wishfest

#endif // WISHFEST_SERIALIZER_H
