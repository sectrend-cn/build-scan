/*
 * Copyright (C) 2018-2020  SECTREND.COM.CN
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.sectrend.buildscan.buildTools.scanner;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class BlacklistRules {

    public static final List<String> FILTERED_EXT;

    public static final List<String> ONLY_WFP;

    public static final List<String> FILTERED_DIRS;

   // public static final List<String> FILTERED_DIR_EXT;

    public static final List<String> FILTERED_FILES;
    private static final List<String> BUILD_FILE;

    private static final List<String> SO_BINARY_FILE;

    private static final  List<String> WHITE_FILE;

    static {

        /**
         * 什么都不生成
         */
        //要跳过的文件扩展名
        FILTERED_EXT = Arrays.asList( ".1", ".2", ".3", ".4", ".5", ".6", ".7", ".8", ".9", ".ac", ".adoc", ".am",
                ".asc", ".asciidoc", ".bmp", ".build", ".cfg", ".chm", ".class", ".cmake",
                ".cnf", ".conf", ".config", ".contributors", ".copying", ".crt", ".csproj",
                ".css", ".csv", ".cvsignore", ".dat", ".data", ".db", ".doc", ".ds_store",
                ".dtd", ".dts", ".dtsi", ".dump", ".eot", ".eps", ".geojson", ".gdoc", ".gif",
                ".gitignore", ".glif", ".gmo", ".guess", ".hex", ".htm", ".html",
                ".ico", ".in", ".inc", ".info", ".ini", ".ipynb", ".jpeg", ".jpg",
                ".jsonld", ".log", ".m4", ".map", ".markdown", ".md", ".md5", ".meta", ".mk",
                ".mxml", ".o", ".otf", ".out", ".pbtxt", ".pdf", ".pem", ".phtml", ".plist",
                ".png", ".po", ".ppt", ".prefs", ".properties", ".pyc", ".qdoc", ".result",
                ".rgb",".rst", ".rtf", ".scss", ".sha", ".sha1", ".sha2", ".sha256", ".sln",
                ".spec", ".sql", ".sub", ".svg", ".svn-base", ".tab", ".template", ".test",
                ".tex", ".tiff", ".toml", ".ttf", ".txt", ".utf-8", ".vim", ".wav", ".whl",
                ".woff", ".xht", ".xhtml", ".xls", ".xpm", ".xsd", ".xul", ".yaml",
                ".yml", ".LAS",".adk",".asc",".cif",".cli",".cosmo",".deploy",
                ".dfm",".dmm",".fa",".fasta",".fcb",".flm",".fna",".gbr",".gen",".gro",
                ".hgtags",".hh",".ihex",".kp",".mpx",".pdb",".poly",".prn",".ps",".ref",
                ".resx",".smp",".stg",".tfa",".tsv",".vcf",".vhd",".xy",".xyz",

                "-DOC", "CHANGELOG", "CONFIG", "COPYING", "COPYING.LIB",
                "LICENSE.MD", "LICENSE.TXT", "MAKEFILE", "NOTICE", "NOTICE",
                "README", "SWIFTDOC", "TEXIDOC", "TODO", "VERSION"
        );

        //要跳过的文件夹
        FILTERED_DIRS = Arrays.asList(
                "svn","hg","bzr","cvs","git",  "repo" ,"." ,"..", "dist-info", "egg-info"
        );
   /*     //要跳过的文件夹结尾
        FILTERED_DIR_EXT = Arrays.asList(
                "egg-info"
        );*/

        //要跳过的文件名
        FILTERED_FILES = Arrays.asList(
                "gradlew", "gradlew.bat", "mvnw", "mvnw.cmd", "gradle-wrapper.jar", "maven-wrapper.jar",
                "thumbs.db", "babel.config.js",
                "copying.lib", "makefile", "Makefile", "config", "NOTICE", ".git"
        );

        WHITE_FILE = Arrays.asList(
                "license", "licenses", "licence", "licences","Pipfile","requirements.txt","control"/*,"CMakeLists.txt"*/
        );

//======================================================================================================================
        /**
         *只生成MD5不生产指纹
         */
        ONLY_WFP = Arrays.asList("exe", "zip", "tar", "tgz", "gz", "7z", "rar", "jar", "war", "ear", "class", "pyc",
                "o", "a", "so", "obj", "dll", "lib", "out", "app", "bin",
                "lst", "dat", "json", "htm", "html", "xml", "md", "txt",
                "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "pages", "key", "numbers",
                "pdf", "minjs", "mf", "MF", "EXE", "ZIP", "TAR", "TGZ", "GZ", "7Z", "RAR", "JAR", "WAR", "EAR", "CLASS", "PYC",
                "O", "A", "SO", "OBJ", "DLL", "LIB", "OUT", "APP", "BIN",
                "LST", "DAT", "JSON", "HTM", "HTML", "XML", "MD", "TXT",
                "DOC", "DOCX", "XLS", "XLSX", "PPT", "PPTX", "ODT", "ODS", "ODP", "PAGES", "KEY", "NUMBERS",
                "PDF", "MINJS");

        /**
         *  多包多项目规则
         */
        BUILD_FILE = Arrays.asList("Pipfile",  "requirements", "setup.py", "Pipfile.lock", "package.json",
                "npm-shrinkwrap.json", "package-lock.json", "pom.xml", "build.gradle", "go.mod", "environment.yml");


        /**
         * SO文件
         */
        SO_BINARY_FILE = Arrays.asList(".so", ".so.");
    }

    public static boolean isMarkupOrJSON(String src) {
        return ((src != null && src.length() > 0) && (src.charAt(0) == '{' || src.startsWith("<?xml") || src.startsWith("<html") || src.startsWith("<AC3D")));
    }

    public static boolean hasBlacklistedExt(String filename) {
        boolean flag = false;
        for (String s : FILTERED_EXT) {
            if(filename.toLowerCase().endsWith(s.toLowerCase())){
                flag = true;
            };
        }
        return StringUtils.isNotEmpty(filename) && (flag || filename.startsWith("."));

    }

    public static boolean GenerateOnlyWfp(String filename) {
        return StringUtils.isNotEmpty(filename) && ONLY_WFP.contains(FilenameUtils.getExtension(filename));

    }

    public static boolean filteredDirs(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        //return StringUtils.isNotEmpty(filename) && (FILTERED_DIRS.contains(extension));
        return StringUtils.isNotEmpty(filename) && FILTERED_DIRS.contains(FilenameUtils.getExtension(filename));
    }

    public static boolean filteredFiles(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        return StringUtils.isNotEmpty(filename) && (FILTERED_FILES.contains(filename) || filename.startsWith("."));
    }

    public static boolean filteredWhiteFile(String filename) {
        String white_file = FilenameUtils.getExtension(filename);
        return StringUtils.isNotEmpty(filename) && contrast(filename);

    }

    public static boolean isSoFile(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        return fileName.endsWith(".so") || fileName.contains(".so.");
    }

    private static boolean contrast(String filename) {

        for (String s : WHITE_FILE) {
            if(s.toLowerCase().equals(filename.toLowerCase())){
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        File file = new File("D:\\tmp\\A-Tune-master");
        boolean b = contrast(file.getName());
        System.out.println("b = " + b);
    }
}
