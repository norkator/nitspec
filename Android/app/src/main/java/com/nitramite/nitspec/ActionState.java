package com.nitramite.nitspec;

class ActionState {

    // Service broadcast actions, which are used from services to trigger messages to main ui
    static final String ACTION_STATE_NONE = "ACTION_STATE_NONE";
    static final String ACTION_STATE_CONNECTING = "ACTION_STATE_CONNECTING";
    static final String ACTION_STATE_CONNECTED = "ACTION_STATE_CONNECTED";
    static final String ACTION_CANCEL_DISCOVERY = "ACTION_CANCEL_DISCOVERY";
    static final String ACTION_CONNECTION_FAILED = "ACTION_CONNECTION_FAILED";
    static final String ACTION_CONNECTION_LOST = "ACTION_CONNECTION_LOST";
    static final String ACTION_SCOPE_MESSAGE_IN = "ACTION_SCOPE_MESSAGE_IN";

} // End of class