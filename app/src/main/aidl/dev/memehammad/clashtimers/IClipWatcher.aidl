package dev.memehammad.clashtimers;

// Shizuku user service: runs with shell rights and watches the clipboard for village exports.
interface IClipWatcher {
    // Token the watcher must send back with each export, so the receiver ignores anything else.
    void setToken(String token) = 1;

    // Reserved transaction code Shizuku uses to stop a user service.
    void destroy() = 16777114;
}
