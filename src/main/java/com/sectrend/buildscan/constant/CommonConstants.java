package com.sectrend.buildscan.constant;

import java.util.regex.Pattern;

public interface CommonConstants {
    String INCOMPATIBLE_SUFFIX = "+incompatible";

    String SHA1_REGEX = "[a-fA-F0-9]{40}";

    String SHORT_SHA1_REGEX = "[a-fA-F0-9]{12}";

    String GIT_VERSION_FORMAT = ".*(%s).*";

    Pattern SHA1_VERSION_PATTERN = Pattern.compile(String.format(GIT_VERSION_FORMAT, SHA1_REGEX));

    Pattern SHORT_SHA1_VERSION_PATTERN = Pattern.compile(String.format(GIT_VERSION_FORMAT, SHORT_SHA1_REGEX));
}
