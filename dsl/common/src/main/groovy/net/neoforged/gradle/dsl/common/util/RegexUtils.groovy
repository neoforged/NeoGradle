package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic

import java.util.regex.Pattern

import static java.util.regex.Pattern.compile

@CompileStatic
class RegexUtils {

    public static final Pattern REPLACE_PATTERN = compile('^\\{(\\w+)}$')
}
