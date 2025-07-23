package com.sectrend.buildscan.utils;

import lombok.extern.slf4j.Slf4j;

import static com.sectrend.buildscan.utils.Encoding.*;


/**
 * Detect encoding
 *
 * @author wuguangya
 * @date 23/8/22
 */
@Slf4j
public class StringEncodingDetector {

    public static String detectCharset(byte[] rawText) {
        int encodingIndex = detectEncoding(rawText);
        String charset = encodingReadableNames[encodingIndex];
        if (charset == null) {
            charset = encodingReadableNames[UTF8];
        }
        return charset;
    }

    /**
     * 函数 ： detectEncoding Aruguments： 字节数组 返回 ： Encoding 枚举中的编码之一 （GB2312， HZ，
     * BIG5、EUC_TW、ASCII 或其他）描述：此函数查看字节数组并为其分配一个概率分数
     * 每种编码类型。返回概率最高的编码类型。
     */
    public static int detectEncoding(byte[] rawText) {
        int[] encodingScores;
        int index, highestScore = 0;
        int detectedEncoding = OTHER;
        encodingScores = new int[ENCODING_TYPE_COUNT];
        // Assign Scores
        encodingScores[GB2312] = calculateGb2312Score(rawText);
        encodingScores[GBK] = calculateGbkScore(rawText);
        encodingScores[GB18030] = calculateGb18030Score(rawText);
        encodingScores[HZ] = calculateHzScore(rawText);
        encodingScores[BIG5] = calculateBig5Score(rawText);
        encodingScores[CNS11643] = calculateEucTwScore(rawText);
        encodingScores[ISO2022CN] = calculateIso2022CnScore(rawText);
        encodingScores[UTF8] = calculateUtf8Score(rawText);
        encodingScores[UNICODE] = calculateUtf16Score(rawText);
        encodingScores[EUC_KR] = calculateEucKrScore(rawText);
        encodingScores[CP949] = calculateCp949Score(rawText);
        encodingScores[JOHAB] = 0;
        encodingScores[ISO2022KR] = calculateIso2022KrScore(rawText);
        encodingScores[ASCII] = calculateAsciiScore(rawText);
        encodingScores[SJIS] = calculateSjisScore(rawText);
        encodingScores[EUC_JP] = calculateEucJpScore(rawText);
        encodingScores[ISO2022JP] = calculateIso2022JpScore(rawText);
        encodingScores[UNICODET] = 0;
        encodingScores[UNICODES] = 0;
        encodingScores[ISO2022CN_GB] = 0;
        encodingScores[ISO2022CN_CNS] = 0;
        encodingScores[OTHER] = 0;
        // 将分数制成表格
        for (index = 0; index < ENCODING_TYPE_COUNT; index++) {
            if (encodingScores[index] > highestScore) {
                detectedEncoding = index;
                highestScore = encodingScores[index];
            }
        }
        // 如果没有任何分数高于 50，则返回 OTHER
        if (highestScore <= 50) {
            detectedEncoding = OTHER;
        }
        return detectedEncoding;
    }

    /**
     * 函数： calculateGb2312Score 参数： 指向字节数组的指针 返回： 从 0 到 100 的数字，表示概率文本
     * in array 使用 GB-2312 编码
     */
    private static int calculateGb2312Score(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, gbChars = 1;
        long gbFreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column;
        // Stage 1: 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0) {
                dbChars++;
                if ((byte) 0xA1 <= rawText[i] && rawText[i] <= (byte) 0xF7 && (byte) 0xA1 <= rawText[i + 1]
                        && rawText[i + 1] <= (byte) 0xFE) {
                    gbChars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0xA1;
                    column = rawText[i + 1] + 256 - 0xA1;
                    if (GBEncodingFreq[row][column] != 0) {
                        gbFreq += GBEncodingFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        // 在 GB 高频字符范围内
                        gbFreq += 200;
                    }
                }
                i++;
            }
        }
        rangeVal = 50 * ((float) gbChars / (float) dbChars);
        freqVal = 50 * ((float) gbFreq / (float) totalFreq);
        return (int) (rangeVal + freqVal);
    }

    /**
     * 函数： calculateGbkScore 参数： 指向字节数组的指针 返回： 从 0 到 100 的数字，表示 中的概率文本
     * 数组使用 GBK 编码
     */
    private static int calculateGbkScore(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, gbChars = 1;
        long gbFreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column;
        // Stage 1: 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0){
                dbChars++;
                if ((byte) 0xA1 <= rawText[i] && rawText[i] <= (byte) 0xF7 && // Original GB range
                        (byte) 0xA1 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xFE) {
                    gbChars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0xA1;
                    column = rawText[i + 1] + 256 - 0xA1;

                    if (GBEncodingFreq[row][column] != 0) {
                        gbFreq += GBEncodingFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        gbFreq += 200;
                    }
                } else if ((byte) 0x81 <= rawText[i]
                        && rawText[i] <= (byte) 0xFE
                        && // Extended GB range
                        (((byte) 0x80 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xFE) || ((byte) 0x40 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0x7E))) {
                    gbChars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0x81;
                    if (0x40 <= rawText[i + 1] && rawText[i + 1] <= 0x7E) {
                        column = rawText[i + 1] - 0x40;
                    } else {
                        column = rawText[i + 1] + 256 - 0x40;
                    }

                    if (GBKEncodingFreq[row][column] != 0) {
                        gbFreq += GBKEncodingFreq[row][column];
                    }
                }
                i++;
            }
        }
        rangeVal = 50 * ((float) gbChars / (float) dbChars);
        freqVal = 50 * ((float) gbFreq / (float) totalFreq);
        // 对于常规 GB 文件，这将给出相同的分数，因此稍微限制了它
        return (int) (rangeVal + freqVal) - 1;
    }

    /**
     * 函数： calculateGb18030Score 参数： 指向字节数组的指针 返回： 从 0 到 100 的数字，表示概率文本
     * 数组中的使用 GBK 编码
     */
    private static int calculateGb18030Score(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, gbChars = 1;
        long gbFreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column;
        // Stage 1: 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0){
                dbChars++;
                if ((byte) 0xA1 <= rawText[i] && rawText[i] <= (byte) 0xF7 && // Original GB range
                        i + 1 < rawTextLen && (byte) 0xA1 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xFE) {
                    gbChars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0xA1;
                    column = rawText[i + 1] + 256 - 0xA1;
                    if (GBEncodingFreq[row][column] != 0) {
                        gbFreq += GBEncodingFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        gbFreq += 200;
                    }
                } else if ((byte) 0x81 <= rawText[i] && rawText[i] <= (byte) 0xFE
                        && // Extended GB range
                        i + 1 < rawTextLen
                        && (((byte) 0x80 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xFE) || ((byte) 0x40 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0x7E))) {
                    gbChars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0x81;
                    if (0x40 <= rawText[i + 1] && rawText[i + 1] <= 0x7E) {
                        column = rawText[i + 1] - 0x40;
                    } else {
                        column = rawText[i + 1] + 256 - 0x40;
                    }
                    if (GBKEncodingFreq[row][column] != 0) {
                        gbFreq += GBKEncodingFreq[row][column];
                    }
                } else if ((byte) 0x81 <= rawText[i]
                        && rawText[i] <= (byte) 0xFE
                        && // Extended GB range
                        i + 3 < rawTextLen && (byte) 0x30 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0x39
                        && (byte) 0x81 <= rawText[i + 2] && rawText[i + 2] <= (byte) 0xFE && (byte) 0x30 <= rawText[i + 3]
                        && rawText[i + 3] <= (byte) 0x39) {
                    gbChars++;
                }
                i++;
            }
        }
        rangeVal = 50 * ((float) gbChars / (float) dbChars);
        freqVal = 50 * ((float) gbFreq / (float) totalFreq);
        // 对于常规 GB 文件，这将给出相同的分数，因此稍微限制了它
        return (int) (rangeVal + freqVal) - 1;
    }

    /**
     * 函数: calculateHzScore 参数：字节数组返回：从 0 到 100 的数字，表示数组中文本的概率，使用 HZ 编码
     */
    private static int calculateHzScore(byte[] rawText) {
        int i, rawTextLen;
        int hzChars = 0, dbChars = 1;
        long hzFreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int hzStart = 0, hzEnd = 0;
        int row, column;
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] == '~') {
                if (rawText[i + 1] == '{') {
                    hzStart++;
                    i += 2;
                    while (i < rawTextLen - 1) {
                        if (rawText[i] == 0x0A || rawText[i] == 0x0D) {
                            break;
                        } else if (rawText[i] == '~' && rawText[i + 1] == '}') {
                            hzEnd++;
                            i++;
                            break;
                        } else if ((0x21 <= rawText[i] && rawText[i] <= 0x77) && (0x21 <= rawText[i + 1] && rawText[i + 1] <= 0x77)) {
                            hzChars += 2;
                            row = rawText[i] - 0x21;
                            column = rawText[i + 1] - 0x21;
                            totalFreq += 500;
                            if (GBEncodingFreq[row][column] != 0) {
                                hzFreq += GBEncodingFreq[row][column];
                            } else if (15 <= row && row < 55) {
                                hzFreq += 200;
                            }
                        } else if ((0xA1 <= rawText[i] && rawText[i] <= 0xF7) && (0xA1 <= rawText[i + 1] && rawText[i + 1] <= 0xF7)) {
                            hzChars += 2;
                            row = rawText[i] + 256 - 0xA1;
                            column = rawText[i + 1] + 256 - 0xA1;
                            totalFreq += 500;
                            if (GBEncodingFreq[row][column] != 0) {
                                hzFreq += GBEncodingFreq[row][column];
                            } else if (15 <= row && row < 55) {
                                hzFreq += 200;
                            }
                        }
                        dbChars += 2;
                        i += 2;
                    }
                } else if (rawText[i + 1] == '}') {
                    hzEnd++;
                    i++;
                } else if (rawText[i + 1] == '~') {
                    i++;
                }
            }
        }
        if (hzStart > 4) {
            rangeVal = 50;
        } else if (hzStart > 1) {
            rangeVal = 41;
        } else if (hzStart > 0) { // Only 39 in case the sequence happened to occur
            rangeVal = 39; // in otherwise non-Hz text
        } else {
            rangeVal = 0;
        }
        freqVal = 50 * ((float) hzFreq / (float) totalFreq);
        return (int) (rangeVal + freqVal);
    }

    /**
     * 函数： calculateBig5Score 参数： 字节数组 返回 ： 从 0 到 100 的数字，表示数组中的概率文本
     * Big5 编码
     */
    private static int calculateBig5Score(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, bfchars = 1;
        float rangeVal = 0, freqVal = 0;
        long bffreq = 0, totalFreq = 1;
        int row, column;
        // 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0){
                dbChars++;
                if ((byte) 0xA1 <= rawText[i]
                        && rawText[i] <= (byte) 0xF9
                        && (((byte) 0x40 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0x7E) || ((byte) 0xA1 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xFE))) {
                    bfchars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0xA1;
                    if (0x40 <= rawText[i + 1] && rawText[i + 1] <= 0x7E) {
                        column = rawText[i + 1] - 0x40;
                    } else {
                        column = rawText[i + 1] + 256 - 0x61;
                    }
                    if (Big5EncodingFreq[row][column] != 0) {
                        bffreq += Big5EncodingFreq[row][column];
                    } else if (3 <= row && row <= 37) {
                        bffreq += 200;
                    }
                }
                i++;
            }
        }
        rangeVal = 50 * ((float) bfchars / (float) dbChars);
        freqVal = 50 * ((float) bffreq / (float) totalFreq);
        return (int) (rangeVal + freqVal);
    }

    /**
     * 函数： calculateEucTwScore 参数： 字节数组 返回 ： 从 0 到 100 的数字，表示数组中的概率文本
     * 使用 EUC-TW （CNS 11643） 编码
     */
    private static int calculateEucTwScore(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, cnschars = 1;
        long cnsfreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column;
        // 检查字符是否符合可接受的范围并具有预期的使用频率
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0){
                dbChars++;
                if (i + 3 < rawTextLen && (byte) 0x8E == rawText[i] && (byte) 0xA1 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xB0
                        && (byte) 0xA1 <= rawText[i + 2] && rawText[i + 2] <= (byte) 0xFE && (byte) 0xA1 <= rawText[i + 3]
                        && rawText[i + 3] <= (byte) 0xFE) { // Planes 1 - 16
                    cnschars++;
                    // 这些都是不太常见的字符，所以就忽略 freq
                    i += 3;
                } else if ((byte) 0xA1 <= rawText[i] && rawText[i] <= (byte) 0xFE && // Plane 1
                        (byte) 0xA1 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xFE) {
                    cnschars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0xA1;
                    column = rawText[i + 1] + 256 - 0xA1;
                    if (EUC_TWEncodingFreq[row][column] != 0) {
                        cnsfreq += EUC_TWEncodingFreq[row][column];
                    } else if (35 <= row && row <= 92) {
                        cnsfreq += 150;
                    }
                    i++;
                }
            }
        }
        rangeVal = 50 * ((float) cnschars / (float) dbChars);
        freqVal = 50 * ((float) cnsfreq / (float) totalFreq);
        return (int) (rangeVal + freqVal);
    }

    /**
     * 函数： calculateIso2022CnScore 参数： 字节数组 返回 ： 从 0 到 100 的数字，表示
     * 数组使用 ISO 2022-CN 编码适用于基本情况，但仍需要更多工作
     */
    private static int calculateIso2022CnScore(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, isochars = 1;
        long isoFreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column;
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] == (byte) 0x1B && i + 3 < rawTextLen) { // Escape char ESC
                if (rawText[i + 1] == (byte) 0x24 && rawText[i + 2] == 0x29 && rawText[i + 3] == (byte) 0x41) { // GB Escape $ ) A
                    i += 4;
                    while (rawText[i] != (byte) 0x1B) {
                        dbChars++;
                        if ((0x21 <= rawText[i] && rawText[i] <= 0x77) && (0x21 <= rawText[i + 1] && rawText[i + 1] <= 0x77)) {
                            isochars++;
                            row = rawText[i] - 0x21;
                            column = rawText[i + 1] - 0x21;
                            totalFreq += 500;
                            if (GBEncodingFreq[row][column] != 0) {
                                isoFreq += GBEncodingFreq[row][column];
                            } else if (15 <= row && row < 55) {
                                isoFreq += 200;
                            }
                            i++;
                        }
                        i++;
                    }
                } else if (i + 3 < rawTextLen && rawText[i + 1] == (byte) 0x24 && rawText[i + 2] == (byte) 0x29
                        && rawText[i + 3] == (byte) 0x47) {
                    // CNS Escape $ ) G
                    i += 4;
                    while (rawText[i] != (byte) 0x1B) {
                        dbChars++;
                        if ((byte) 0x21 <= rawText[i] && rawText[i] <= (byte) 0x7E && (byte) 0x21 <= rawText[i + 1]
                                && rawText[i + 1] <= (byte) 0x7E) {
                            isochars++;
                            totalFreq += 500;
                            row = rawText[i] - 0x21;
                            column = rawText[i + 1] - 0x21;
                            if (EUC_TWEncodingFreq[row][column] != 0) {
                                isoFreq += EUC_TWEncodingFreq[row][column];
                            } else if (35 <= row && row <= 92) {
                                isoFreq += 150;
                            }
                            i++;
                        }
                        i++;
                    }
                }
                if (rawText[i] == (byte) 0x1B && i + 2 < rawTextLen && rawText[i + 1] == (byte) 0x28 && rawText[i + 2] == (byte) 0x42) { // ASCII:
                    // ESC
                    // ( B
                    i += 2;
                }
            }
        }
        rangeVal = 50 * ((float) isochars / (float) dbChars);
        freqVal = 50 * ((float) isoFreq / (float) totalFreq);

        return (int) (rangeVal + freqVal);
    }

    /**
     * 函数： calculateUtf8Score 参数： 字节数组 返回 ： 从 0 到 100 的数字，表示数组中的概率文本
     * Unicode 的 UTF-8 编码
     */
    private static int calculateUtf8Score(byte[] rawText) {
        int utf8Score = 0;
        int i, rawTextLen = 0;
        int goodbytes = 0, asciibytes = 0;
        // 可能也使用 UTF8 字节顺序标记：EF BB BF
        // 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen; i++) {
            if ((rawText[i] & (byte) 0x7F) == rawText[i]) { // One byte
                asciibytes++;
                // 忽略 ASCII，可以丢弃计数
            } else if (-64 <= rawText[i] && rawText[i] <= -33 && // Two bytes
                    i + 1 < rawTextLen && -128 <= rawText[i + 1] && rawText[i + 1] <= -65) {
                goodbytes += 2;
                i++;
            } else if (-32 <= rawText[i] && rawText[i] <= -17
                    && // Three bytes
                    i + 2 < rawTextLen && -128 <= rawText[i + 1] && rawText[i + 1] <= -65 && -128 <= rawText[i + 2]
                    && rawText[i + 2] <= -65) {
                goodbytes += 3;
                i += 2;
            }
        }
        if (asciibytes == rawTextLen) {
            return 0;
        }
        utf8Score = (int) (100 * ((float) goodbytes / (float) (rawTextLen - asciibytes)));

        if (utf8Score > 98) {
            return utf8Score;
        } else if (utf8Score > 95 && goodbytes > 30) {
            return utf8Score;
        } else {
            return 0;
        }
    }

    /**
     * 函数： calculateUtf16Score 参数： 字节数组 返回 ： 从 0 到 100 的数字，表示数组使用中的概率文本
     * Unicode 的 UTF-16 编码，基于 BOM 的猜测 // 不是很通用，需要更多的工作
     */
    private static int calculateUtf16Score(byte[] rawText) {

        if (rawText.length > 1 && ((byte) 0xFE == rawText[0] && (byte) 0xFF == rawText[1]) || // Big-endian
                ((byte) 0xFF == rawText[0] && (byte) 0xFE == rawText[1])) { // Little-endian
            return 100;
        }
        return 0;
    }

    /**
     * 函数： calculateAsciiScore 参数： 字节数组 返回： 从 0 到 100 的数字，表示数组中的概率文本
     * 所有 ASCII 描述：查看数组是否有任何字符不在 ASCII 范围内，如果有，则分数降低
     */
    private static int calculateAsciiScore(byte[] rawText) {
        int asciiScore = 75;
        int i, rawTextLen;
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen; i++) {
            if (rawText[i] < 0) {
                asciiScore = asciiScore - 5;
            } else if (rawText[i] == (byte) 0x1B) { // ESC (used by ISO 2022)
                asciiScore = asciiScore - 5;
            }
            if (asciiScore <= 0) {
                return 0;
            }
        }
        return asciiScore;
    }

    /**
     * 函数： calculateEucKrScore 参数： 指向字节数组的指针 返回： 从 0 到 100 的数字，表示概率文本数组中的
     * 使用 EUC-KR 编码
     */
    private static int calculateEucKrScore(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, krChars = 1;
        long krFreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column;
        // Stage 1: 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0){
                dbChars++;
                if ((byte) 0xA1 <= rawText[i] && rawText[i] <= (byte) 0xFE && (byte) 0xA1 <= rawText[i + 1]
                        && rawText[i + 1] <= (byte) 0xFE) {
                    krChars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0xA1;
                    column = rawText[i + 1] + 256 - 0xA1;
                    if (KREncodingFreq[row][column] != 0) {
                        krFreq += KREncodingFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        krFreq += 0;
                    }
                }
                i++;
            }
        }
        rangeVal = 50 * ((float) krChars / (float) dbChars);
        freqVal = 50 * ((float) krFreq / (float) totalFreq);
        return (int) (rangeVal + freqVal);
    }

    /**
     * 函数： cp949__probability 参数： 指向字节数组的指针 返回： 从 0 到 100 的数字，表示概率文本
     * in array 使用 Cp949 编码
     */
    private static int calculateCp949Score(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, krchars = 1;
        long krfreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column;
        // Stage 1: 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0){
                dbChars++;
                if ((byte) 0x81 <= rawText[i]
                        && rawText[i] <= (byte) 0xFE
                        && ((byte) 0x41 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0x5A || (byte) 0x61 <= rawText[i + 1]
                        && rawText[i + 1] <= (byte) 0x7A || (byte) 0x81 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xFE)) {
                    krchars++;
                    totalFreq += 500;
                    if ((byte) 0xA1 <= rawText[i] && rawText[i] <= (byte) 0xFE && (byte) 0xA1 <= rawText[i + 1]
                            && rawText[i + 1] <= (byte) 0xFE) {
                        row = rawText[i] + 256 - 0xA1;
                        column = rawText[i + 1] + 256 - 0xA1;
                        if (KREncodingFreq[row][column] != 0) {
                            krfreq += KREncodingFreq[row][column];
                        }
                    }
                }
                i++;
            }
        }
        rangeVal = 50 * ((float) krchars / (float) dbChars);
        freqVal = 50 * ((float) krfreq / (float) totalFreq);
        return (int) (rangeVal + freqVal);
    }

    private static int calculateIso2022KrScore(byte[] rawText) {
        int i;
        for (i = 0; i < rawText.length; i++) {
            if (i + 3 < rawText.length && rawText[i] == 0x1b && (char) rawText[i + 1] == '$' && (char) rawText[i + 2] == ')'
                    && (char) rawText[i + 3] == 'C') {
                return 100;
            }
        }
        return 0;
    }

    /**
     * 函数： euc_jp_probability 参数： 指向字节数组的指针 返回： 从 0 到 100 的数字，表示概率文本数组中的
     * 使用 EUC-JP 编码
     */
    private static int calculateEucJpScore(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, jpChars = 1;
        long jpfreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column;
        // Stage 1: 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0){
                dbChars++;
                if ((byte) 0xA1 <= rawText[i] && rawText[i] <= (byte) 0xFE && (byte) 0xA1 <= rawText[i + 1]
                        && rawText[i + 1] <= (byte) 0xFE) {
                    jpChars++;
                    totalFreq += 500;
                    row = rawText[i] + 256 - 0xA1;
                    column = rawText[i + 1] + 256 - 0xA1;
                    if (JPEncodingFreq[row][column] != 0) {
                        jpfreq += JPEncodingFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        jpfreq += 0;
                    }
                }
                i++;
            }
        }
        rangeVal = 50 * ((float) jpChars / (float) dbChars);
        freqVal = 50 * ((float) jpfreq / (float) totalFreq);
        return (int) (rangeVal + freqVal);
    }

    private static int calculateIso2022JpScore(byte[] rawText) {
        int i;
        for (i = 0; i < rawText.length; i++) {
            if (i + 2 < rawText.length && rawText[i] == 0x1b && (char) rawText[i + 1] == '$' && (char) rawText[i + 2] == 'B') {
                return 100;
            }
        }
        return 0;
    }

    /**
     * 函数： calculateSjisScore 参数： 指向字节数组的指针 返回 ： 从 0 到 100 的数字，表示概率文本
     * 数组使用 Shift-JIS 编码
     */
    private static int calculateSjisScore(byte[] rawText) {
        int i, rawTextLen = 0;
        int dbChars = 1, jpChars = 1;
        long jpFreq = 0, totalFreq = 1;
        float rangeVal = 0, freqVal = 0;
        int row, column, adjust;

        // Stage 1: 检查字符是否符合可接受的范围
        rawTextLen = rawText.length;
        for (i = 0; i < rawTextLen - 1; i++) {
            if (rawText[i] < 0){
                dbChars++;
                if (i + 1 < rawText.length
                        && (((byte) 0x81 <= rawText[i] && rawText[i] <= (byte) 0x9F) || ((byte) 0xE0 <= rawText[i] && rawText[i] <= (byte) 0xEF))
                        && (((byte) 0x40 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0x7E) || ((byte) 0x80 <= rawText[i + 1] && rawText[i + 1] <= (byte) 0xFC))) {
                    jpChars++;
                    totalFreq += 500;
                    row = rawText[i] + 256;
                    column = rawText[i + 1] + 256;
                    if (column < 0x9f) {
                        adjust = 1;
                        if (column > 0x7f) {
                            column -= 0x20;
                        } else {
                            column -= 0x19;
                        }
                    } else {
                        adjust = 0;
                        column -= 0x7e;
                    }
                    if (row < 0xa0) {
                        row = ((row - 0x70) << 1) - adjust;
                    } else {
                        row = ((row - 0xb0) << 1) - adjust;
                    }
                    row -= 0x20;
                    column = 0x20;
                    if (row < JPEncodingFreq.length && column < JPEncodingFreq[row].length && JPEncodingFreq[row][column] != 0) {
                        jpFreq += JPEncodingFreq[row][column];
                    }
                    i++;
                }
            }
        }
        rangeVal = 50 * ((float) jpChars / (float) dbChars);
        freqVal = 50 * ((float) jpFreq / (float) totalFreq);
        // 对于常规 GB 文件，这将给出相同的分数，因此稍微限制了它
        return (int) (rangeVal + freqVal) - 1;
    }
}
