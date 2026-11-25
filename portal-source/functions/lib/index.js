"use strict";
/**
 * Asheq Ullah Rafiq Digital Portal — Cloud Functions
 * Project: asheq-ullah-rafiq-digital-p
 * Database: ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 *
 * Implements server-side FCM push notification triggers:
 * 1. onNoticeWritten: Dispatches broadcast notifications when notices are published.
 * 2. onApplicationUpdated: Dispatches personalized status notifications to citizens upon application status transitions.
 * 3. onAppointmentUpdated: Mirror collection trigger with automated deduplication.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.setMessagingForTesting = exports.setDbForTesting = exports.getFCM = exports.getDb = exports.getAdminApp = exports.UserPreferenceService = exports.MessagingService = exports.IdempotencyService = exports.CONFIG = exports.handleApplicationStatusChangeLogic = exports.onAppointmentUpdated = exports.onApplicationUpdated = exports.handleNoticeWrittenLogic = exports.onNoticeWritten = void 0;
var onNoticeCreated_1 = require("./triggers/onNoticeCreated");
Object.defineProperty(exports, "onNoticeWritten", { enumerable: true, get: function () { return onNoticeCreated_1.onNoticeWritten; } });
Object.defineProperty(exports, "handleNoticeWrittenLogic", { enumerable: true, get: function () { return onNoticeCreated_1.handleNoticeWrittenLogic; } });
var onApplicationUpdated_1 = require("./triggers/onApplicationUpdated");
Object.defineProperty(exports, "onApplicationUpdated", { enumerable: true, get: function () { return onApplicationUpdated_1.onApplicationUpdated; } });
Object.defineProperty(exports, "onAppointmentUpdated", { enumerable: true, get: function () { return onApplicationUpdated_1.onAppointmentUpdated; } });
Object.defineProperty(exports, "handleApplicationStatusChangeLogic", { enumerable: true, get: function () { return onApplicationUpdated_1.handleApplicationStatusChangeLogic; } });
var config_1 = require("./config");
Object.defineProperty(exports, "CONFIG", { enumerable: true, get: function () { return config_1.CONFIG; } });
var idempotencyService_1 = require("./services/idempotencyService");
Object.defineProperty(exports, "IdempotencyService", { enumerable: true, get: function () { return idempotencyService_1.IdempotencyService; } });
var messagingService_1 = require("./services/messagingService");
Object.defineProperty(exports, "MessagingService", { enumerable: true, get: function () { return messagingService_1.MessagingService; } });
var userPreferenceService_1 = require("./services/userPreferenceService");
Object.defineProperty(exports, "UserPreferenceService", { enumerable: true, get: function () { return userPreferenceService_1.UserPreferenceService; } });
var adminApp_1 = require("./services/adminApp");
Object.defineProperty(exports, "getAdminApp", { enumerable: true, get: function () { return adminApp_1.getAdminApp; } });
Object.defineProperty(exports, "getDb", { enumerable: true, get: function () { return adminApp_1.getDb; } });
Object.defineProperty(exports, "getFCM", { enumerable: true, get: function () { return adminApp_1.getFCM; } });
Object.defineProperty(exports, "setDbForTesting", { enumerable: true, get: function () { return adminApp_1.setDbForTesting; } });
Object.defineProperty(exports, "setMessagingForTesting", { enumerable: true, get: function () { return adminApp_1.setMessagingForTesting; } });
//# sourceMappingURL=index.js.map