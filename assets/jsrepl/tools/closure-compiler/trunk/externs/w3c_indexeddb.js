/*
 * Copyright 2011 PicNet Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Definitions for w3c IndexedDB API. In Chrome all the IndexedDB
 * classes are prefixed with 'webkit'. In order to access constants and static
 * methods of these classes they must be duplicated with the prefix here.
 * @see http://www.w3.org/TR/IndexedDB/
 *
 * @externs
 */

/** @type {IDBFactory} */
Window.prototype.moz_indexedDB;

/** @type {IDBFactory} */
Window.prototype.mozIndexedDB;

/** @type {IDBFactory} */
Window.prototype.webkitIndexedDB;

/** @type {IDBFactory} */
Window.prototype.indexedDB;

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBFactory
 */
function IDBFactory() {}

/**
 * @param {string} name The name of the database to open.
 * @param {string=} opt_description The description of the database.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBFactory.prototype.open = function(name, opt_description) {};

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBDatabaseException
 */
function IDBDatabaseException() {}

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBDatabaseException
 */
function webkitIDBDatabaseException() {}

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.UNKNOWN_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.UNKNOWN_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.NON_TRANSIENT_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.NON_TRANSIENT_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.NOT_FOUND_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.NOT_FOUND_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.CONSTRAINT_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.CONSTRAINT_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.DATA_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.DATA_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.NOT_ALLOWED_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.NOT_ALLOWED_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.TRANSACTION_INACTIVE_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.TRANSACTION_INACTIVE_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.ABORT_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.ABORT_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.READ_ONLY_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.READ_ONLY_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.TIMEOUT_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.TIMEOUT_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.QUOTA_ERR;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.QUOTA_ERR;

/**
 * @const
 * @type {number}
 */
IDBDatabaseException.code;

/**
 * @const
 * @type {number}
 */
webkitIDBDatabaseException.code;

/**
 * @const
 * @type {string}
 */
IDBDatabaseException.message;

/**
 * @const
 * @type {string}
 */
webkitIDBDatabaseException.message;

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBRequest
 */
function IDBRequest() {}

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBRequest
 */
function webkitIDBRequest() {}

/**
 * @type {number}
 * @const
 */
IDBRequest.LOADING;

/**
 * @type {number}
 * @const
 */
webkitIDBRequest.LOADING;

/**
 * @type {number}
 * @const
 */
IDBRequest.DONE;

/**
 * @type {number}
 * @const
 */
webkitIDBRequest.DONE;

/**
 * @type {number}
 * @const
 */
IDBRequest.prototype.readyState;

/**
 * @type {function(Event)}
 */
IDBRequest.prototype.onsuccess = function() {};

/**
 * @type {function(Event)}
 */
IDBRequest.prototype.onerror = function() {};

/**
 * @type {*}
 * @const
 */
IDBRequest.prototype.result;

/**
 * @type {number}
 * @const
 */
IDBRequest.prototype.errorCode;

/**
 * @type {Object}
 * @const
 */
IDBRequest.prototype.source;

/**
 * @type {IDBTransaction}
 * @const
 */
IDBRequest.prototype.transaction;

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBDatabase
 */
function IDBDatabase() {}

/**
 * @type {string}
 * @const
 */
IDBDatabase.prototype.name;

/**
 * @type {string}
 * @const
 */
IDBDatabase.prototype.description;

/**
 * @type {string}
 * @const
 */
IDBDatabase.prototype.version;

/**
 * @type {Array.<string>}
 * @const
 */
IDBDatabase.prototype.objectStoreNames;

/**
 * @param {string} name The name of the object store.
 * @param {Object=} opt_parameters Parameters to be passed
 *     creating the object store.
 * @return {!IDBObjectStore} The created/open object store.
 */
IDBDatabase.prototype.createObjectStore =
    function(name, opt_parameters)  {};

/**
 * @param {string} name The name of the object store to remove.
 */
IDBDatabase.prototype.deleteObjectStore = function(name) {};

/**
 * @param {string} version The new version of the database.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBDatabase.prototype.setVersion = function(version) {};

/**
 * @param {Array.<string>} storeNames The stores to open in this transaction.
 * @param {number=} mode The mode for opening the object stores.
 * @return {!IDBTransaction} The IDBRequest object.
 */
IDBDatabase.prototype.transaction = function(storeNames, mode) {};

/**
 * Closes the database connection.
 */
IDBDatabase.prototype.close = function() {};

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBObjectStore
 */
function IDBObjectStore() {}

/**
 * @type {string}
 * @const
 */
IDBObjectStore.prototype.name;

/**
 * @type {string}
 * @const
 */
IDBObjectStore.prototype.keyPath;

/**
 * @type {Array.<string>}
 * @const
 */
IDBObjectStore.prototype.indexNames;

/**
 * @param {*} value The value to put into the object store.
 * @param {*=} key The key of this value.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBObjectStore.prototype.put = function(value, key) {};

/**
 * @param {*} value The value to add into the object store.
 * @param {*=} key The key of this value.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBObjectStore.prototype.add = function(value, key) {};

/**
 * @param {*} key The key of the document to remove.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBObjectStore.prototype.remove = function(key) {};

/**
 * @param {*} key The key of the document to retreive.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBObjectStore.prototype.get = function(key) {};

/**
 * @param {IDBKeyRange=} range The range of the cursor.
 * @param {number=} direction The direction of cursor enumeration.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBObjectStore.prototype.openCursor = function(range, direction) {};

/**
 * @param {string} name The name of the index.
 * @param {string} keyPath The path to the index key.
 * @param {Object=} opt_paramters Optional parameters
 *     for the created index.
 * @return {!IDBIndex} The IDBIndex object.
 */
IDBObjectStore.prototype.createIndex = function(name, keyPath, opt_paramters) {};

/**
 * @param {string} name The name of the index to retreive.
 * @return {!IDBIndex} The IDBIndex object.
 */
IDBObjectStore.prototype.index = function(name) {};

/**
 * @param {string} indexName The name of the index to remove.
 */
IDBObjectStore.prototype.deleteIndex = function(indexName) {};

/**
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBObjectStore.prototype.clear = function() {};

/**
 * Note: This is currently only supported by Mozilla's implementation
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBObjectStore.prototype.getAll = function() {};

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBIndex
 */
function IDBIndex() {}

/**
 * @type {string}
 * @const
 */
IDBIndex.prototype.name;

/**
 * @type {!IDBObjectStore}
 * @const
 */
IDBIndex.prototype.objectStore;

/**
 * @type {string}
 * @const
 */
IDBIndex.prototype.keyPath;

/**
 * @type {boolean}
 * @const
 */
IDBIndex.prototype.unique;

/**
 * @param {IDBKeyRange=} range The range of the cursor.
 * @param {number=} direction The direction of cursor enumeration.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBIndex.prototype.openCursor = function(range, direction) {};

/**
 * @param {IDBKeyRange=} range The range of the cursor.
 * @param {number=} direction The direction of cursor enumeration.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBIndex.prototype.openKeyCursor = function(range, direction) {};

/**
 * @param {*} key The id of the object to retreive.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBIndex.prototype.get = function(key) {};

/**
 * @param {*} key The id of the object to retreive.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBIndex.prototype.getKey = function(key) {};

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBCursor
 */
function IDBCursor() {}

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBCursor
 */
function webkitIDBCursor() {}

/**
 * @const
 * @type {number}
 */
IDBCursor.NEXT;

/**
 * @const
 * @type {number}
 */
webkitIDBCursor.NEXT;

/**
 * @const
 * @type {number}
 */
IDBCursor.NEXT_NO_DUPLICATE;

/**
 * @const
 * @type {number}
 */
webkitIDBCursor.NEXT_NO_DUPLICATE;

/**
 * @const
 * @type {number}
 */
IDBCursor.PREV;

/**
 * @const
 * @type {number}
 */
webkitIDBCursor.PREV;

/**
 * @const
 * @type {number}
 */
IDBCursor.PREV_NO_DUPLICATE;

/**
 * @const
 * @type {number}
 */
webkitIDBCursor.PREV_NO_DUPLICATE;

/**
 * @type {*}
 * @const
 */
IDBCursor.prototype.source;

/**
 * @type {number}
 * @const
 */
IDBCursor.prototype.direction;

/**
 * @type {*}
 * @const
 */
IDBCursor.prototype.key;

/**
 * @type {number}
 * @const
 */
IDBCursor.prototype.primaryKey;

/**
 * @param {*} value The new value for the current object in the cursor.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBCursor.prototype.update = function(value) {};

/**
 * Note: Must be quoted to avoid parse error.
 * @param {*=} key Continue enumerating the cursor from the specified key
 *    (or next).
 */
IDBCursor.prototype['continue'] = function(key) {};

/**
 * @param {number} count Number of times to iterate the cursor.
 */
IDBCursor.prototype.advance = function(count) {};

/**
 * Note: Must be quoted to avoid parse error.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBCursor.prototype['delete'] = function() {};

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBTransaction
 */
function IDBTransaction() {}

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBTransaction
 */
function webkitIDBTransaction() {}

/**
 * @const
 * @type {number}
 */
IDBTransaction.READ_WRITE;

/**
 * @const
 * @type {number}
 */
webkitIDBTransaction.READ_WRITE;

/**
 * @const
 * @type {number}
 */
IDBTransaction.READ_ONLY;

/**
 * @const
 * @type {number}
 */
webkitIDBTransaction.READ_ONLY;

/**
 * @const
 * @type {number}
 */
IDBTransaction.VERSION_CHANGE;

/**
 * @const
 * @type {number}
 */
webkitIDBTransaction.VERSION_CHANGE;

/**
 * @type {number}
 * @const
 */
IDBTransaction.prototype.mode;

/**
 * @type {IDBDatabase}
 * @const
 */
IDBTransaction.prototype.db;

/**
 * @param {string} name The name of the object store to retreive.
 * @return {!IDBObjectStore} The object store.
 */
IDBTransaction.prototype.objectStore = function(name) {};

/**
 * Aborts the transaction.
 */
IDBTransaction.prototype.abort = function() {};

/**
 * @type {Function}
 */
IDBTransaction.prototype.onabort = function() {};

/**
 * @type {Function}
 */
IDBTransaction.prototype.oncomplete = function() {};

/**
 * @type {Function}
 */
IDBTransaction.prototype.onerror = function() {};

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBKeyRange
 */
function IDBKeyRange() {}

/**
 * @constructor
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBKeyRange
 */
function webkitIDBKeyRange() {}

/**
 * @type {*}
 * @const
 */
IDBKeyRange.prototype.lower;

/**
 * @type {*}
 * @const
 */
IDBKeyRange.prototype.upper;

/**
 * @type {*}
 * @const
 */
IDBKeyRange.prototype.lowerOpen;

/**
 * @type {*}
 * @const
 */
IDBKeyRange.prototype.upperOpen;

/**
 * @param {*} value The single key value of this range.
 * @return {!IDBKeyRange} The key range.
 */
IDBKeyRange.only = function(value) {};

/**
 * @param {*} value The single key value of this range.
 * @return {!IDBKeyRange} The key range.
 */
webkitIDBKeyRange.only = function(value) {};

/**
 * @param {*} bound Creates a lower bound key range.
 * @param {boolean=} open Open the key range.
 * @return {!IDBKeyRange} The key range.
 */
IDBKeyRange.lowerBound = function(bound, open) {};

/**
 * @param {*} bound Creates a lower bound key range.
 * @param {boolean=} open Open the key range.
 * @return {!IDBKeyRange} The key range.
 */
webkitIDBKeyRange.lowerBound = function(bound, open) {};

/**
 * @param {*} bound Creates an upper bound key range.
 * @param {boolean=} open Open the key range.
 * @return {!IDBKeyRange} The key range.
 */
IDBKeyRange.upperBound = function(bound, open) {};

/**
 * @param {*} bound Creates an upper bound key range.
 * @param {boolean=} open Open the key range.
 * @return {!IDBKeyRange} The key range.
 */
webkitIDBKeyRange.upperBound = function(bound, open) {};

/**
 * @param {*} left The left bound value of openLeft is true.
 * @param {*} right The right bound value of openRight is true.
 * @param {boolean=} openLeft Whether to open a left bound range.
 * @param {boolean=} openRight Whether to open a right bound range.
 * @return {!IDBKeyRange} The key range.
 */
IDBKeyRange.bound = function(left, right, openLeft, openRight) {};

/**
 * @param {*} left The left bound value of openLeft is true.
 * @param {*} right The right bound value of openRight is true.
 * @param {boolean=} openLeft Whether to open a left bound range.
 * @param {boolean=} openRight Whether to open a right bound range.
 * @return {!IDBKeyRange} The key range.
 */
webkitIDBKeyRange.bound = function(left, right, openLeft, openRight) {};
