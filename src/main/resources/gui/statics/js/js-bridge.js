/**
 * JsBridge 错误类
 */
class JsBridgeError extends Error {

    constructor(message, identity, argument) {
        super(message); // 调用父类构造函数
        this.name = "JsBridgeError"; // 设置错误名称
        this.identity = identity; // 方法标识符
        this.argument = argument; // 方法参数
    }

    /**
     * 格式化错误信息
     * @returns {string} 格式化后的错误信息
     */
    formatMessage() {
        const identity = this.identity || "N/A";
        const argument = JSON.stringify(this.argument || "N/A");
        return `JsBridgeError: Message="${this.message}", Identity=${identity}, Argument=${argument}`;
    }

}

/**
 * JsBridge 工具库
 */
const JsBridge = {

    /**
     * 调用方法
     * @param {string} identity 方法标识符
     * @param {object} argument 方法参数
     * @returns {Promise} 调用结果
     */
    call: function (identity, argument) {
        return new Promise((resolve, reject) => {
            try {
                if (!window.jsBridge) {
                    throw new JsBridgeError("JsBridge is not initialized!", identity, argument);
                }
                const result = window.jsBridge.call(identity, JSON.stringify(argument));
                resolve(result);
            } catch (error) {
                if (!(error instanceof JsBridgeError)) {
                    error = new JsBridgeError(error.message, identity, argument);
                }
                reject(error);
            }
        });
    },

    /**
     * 检查方法是否已注册
     * @param {string} identity 方法标识符
     * @returns {boolean} 是否已注册
     */
    has: function (identity) {
        if (!window.jsBridge) {
            throw new JsBridgeError("JsBridge is not initialized!", identity, null);
            return false;
        }
        return window.jsBridge.has(identity);
    },

    // 方法完成回调
    completeCallback: null,

    /**
     * 注册方法完成回调
     * @param {function} callback 回调函数
     */
    onComplete: function (callback) {
        completeCallback = callback;
    },

    /**
     * 方法完成回调
     * @param {object} result 回调结果
     */
    completed: function (result) {
        if (completeCallback && typeof completeCallback === "function") {
            completeCallback();
        }
    }

}