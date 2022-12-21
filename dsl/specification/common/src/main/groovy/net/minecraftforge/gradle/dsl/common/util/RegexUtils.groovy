package net.minecraftforge.gradle.dsl.common.util

import java.util.regex.Pattern

import static java.util.regex.Pattern.compile

class RegexUtils {

    public static final Pattern REPLACE_PATTERN = compile('^\\{(\\w+)}$')
}
