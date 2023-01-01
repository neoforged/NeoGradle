package net.minecraftforge.gradle.dsl.generator.transform

import groovy.transform.CompileStatic

@CompileStatic
class Unpluralizer {
    static String unpluralize(String str) {
        if (str.endsWith('s')) {
            return str.dropRight(1)
        } else if (str.endsWith('es')) {
            return str.dropRight(2)
        } else if (str.endsWith('ies')) {
            return str.dropRight(3) + 'y'
        }
        return str
    }
}
