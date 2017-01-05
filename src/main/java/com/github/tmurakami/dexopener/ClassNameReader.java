package com.github.tmurakami.dexopener;

import com.github.tmurakami.dexopener.repackaged.org.ow2.asmdex.lowLevelUtils.DexFileReader;

import java.util.Set;

interface ClassNameReader {
    Set<String> readClassNames(DexFileReader reader);
}