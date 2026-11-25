"use strict";
/**
 * Firebase Admin SDK Initialization Service
 * Explicitly targets named Firestore database:
 * ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 */
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.getAdminApp = getAdminApp;
exports.getDb = getDb;
exports.getFCM = getFCM;
exports.setDbForTesting = setDbForTesting;
exports.setMessagingForTesting = setMessagingForTesting;
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-admin/firestore");
const messaging_1 = require("firebase-admin/messaging");
const config_1 = require("../config");
let appInstance = null;
let firestoreInstance = null;
let messagingInstance = null;
function getAdminApp() {
    if (!appInstance) {
        if (admin.apps.length > 0) {
            appInstance = admin.apps[0];
        }
        else {
            appInstance = admin.initializeApp();
        }
    }
    return appInstance;
}
function getDb() {
    if (!firestoreInstance) {
        const app = getAdminApp();
        try {
            firestoreInstance = (0, firestore_1.getFirestore)(app, config_1.CONFIG.databaseId);
        }
        catch (_err) {
            // Fallback to default if named database fails in local testing
            firestoreInstance = (0, firestore_1.getFirestore)(app);
        }
    }
    return firestoreInstance;
}
function getFCM() {
    if (!messagingInstance) {
        const app = getAdminApp();
        messagingInstance = (0, messaging_1.getMessaging)(app);
    }
    return messagingInstance;
}
// Support injecting custom/mock db or messaging for unit tests
function setDbForTesting(mockDb) {
    firestoreInstance = mockDb;
}
function setMessagingForTesting(mockMessaging) {
    messagingInstance = mockMessaging;
}
//# sourceMappingURL=adminApp.js.map