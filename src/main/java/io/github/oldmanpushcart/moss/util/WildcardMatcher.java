package io.github.oldmanpushcart.moss.util;

/**
 * 通配符匹配工具类
 */
public class WildcardMatcher {

    /**
     * 判断输入字符串是否匹配通配符模式
     *
     * @param input   输入字符串
     * @param pattern 通配符模式（支持 * 和 ?）
     * @return 是否匹配
     */
    public static boolean match(String input, String pattern) {

        if (null == input) {
            return false;
        }

        int n = input.length();
        int m = pattern.length();

        // 动态规划表
        boolean[][] dp = new boolean[n + 1][m + 1];
        dp[0][0] = true;

        // 初始化第一行（处理开头的 *）
        for (int j = 1; j <= m; j++) {
            if (pattern.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 1];
            }
        }

        // 填充动态规划表
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                char pChar = pattern.charAt(j - 1);
                char iChar = input.charAt(i - 1);

                if (pChar == '*') {
                    // * 匹配零个或多个字符
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j];
                } else if (pChar == '?' || pChar == iChar) {
                    // ? 匹配单个字符，或者字符相等
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = false;
                }
            }
        }

        return dp[n][m];
    }

}
