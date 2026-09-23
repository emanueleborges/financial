const path = require("path");
const { getDefaultConfig } = require("expo/metro-config");

const config = getDefaultConfig(__dirname);
const modules = path.resolve(__dirname, "node_modules");

config.resolver.nodeModulesPaths = [modules];
config.resolver.extraNodeModules = {
  "expo-camera": path.resolve(modules, "expo-camera"),
  "expo-file-system": path.resolve(modules, "expo-file-system"),
  "query-string": path.resolve(modules, "query-string"),
  "@expo/vector-icons": path.resolve(modules, "@expo/vector-icons"),
};

module.exports = config;
