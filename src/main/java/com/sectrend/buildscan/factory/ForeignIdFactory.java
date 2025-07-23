
package com.sectrend.buildscan.factory;

import com.sectrend.buildscan.model.ForeignId;
import com.sectrend.buildscan.model.Supplier;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:40
 */
public class ForeignIdFactory {
    public ForeignId createPathForeignId(Supplier supplier, String path) {
        ForeignId foreignId = new ForeignId(supplier);
        foreignId.setPath(path);
        return foreignId;
    }

    public ForeignId createModuleNamesForeignId(Supplier supplier, String... moduleNames) {
        ForeignId foreignId = new ForeignId(supplier);
        foreignId.setModuleNames(moduleNames);
        return foreignId;
    }


    private static final String FILE_STR = "[a-zA-Z]:\\\\(((\\w+)|(\\.\\.))\\\\)*\\w+\\.\\w+";

    //判断是否包含常规的版本定义格式
    private boolean isVersionFormat(String versionInfo) {
        if (versionInfo== null || versionInfo.isEmpty()) {
            return false;
        }
        // 正则表达式，用于匹配版本信息
        String versionPattern = "\\d+(\\.\\d+)+";
        Pattern pattern = Pattern.compile(versionPattern);
        Matcher matcher = pattern.matcher(versionInfo);
        return matcher.find();
    }

    public ForeignId createNameVersionForeignId(Supplier supplier, String name, String version) {
        ForeignId foreignId = new ForeignId(supplier);
        foreignId.setName(name);

        if(StringUtils.isBlank(version) || version.contains("link:") || version.contains("${") || version.contains("file:") || version.contains("*") || version.contains("git")
                 || version.contains("../") || version.contains("http") || Pattern.matches(FILE_STR, version) ){
            version = "undefined";
        } else if (version.contains("||")) {
            String[] versions = version.split("\\|\\|");
            version = selectVersion(versions);
        } else if (version.contains(" ")) {
            String[] versions = version.split(" ");
            version = selectVersion(versions);
        } else if (isOperationSymbols(version)) {
            version = version.substring(2);
        }

       /* if (version.contains("_")) {
            String[] versionInfo = version.split("_"); //10.1.0_@xxx,10.1.0_xxx
            version = versionInfo[0];
        }*/
        if (version.contains("@")) {
            String[] versionInfo = version.split("@"); //npm: abc@1.1.0  gradle:1.0@zip
            version = "";
            for(String v : versionInfo) {
                if (isVersionFormat(v) == true) {
                    version = v;
                    break;
                }
            }
        }
        if (version.isEmpty()) {
            version = "undefined";
        }
        foreignId.setVersion(version);
        return foreignId;
    }

    private String selectVersion(String[] versions)  {
        if (versions == null || versions.length == 0) {
            return "undefined";
        }
        /*Optional<String> optional = Arrays.stream(versions).filter(StringUtils::isNotBlank).findAny();
        if (optional.isPresent()) {
            if (isOperationSymbols(optional.get())) {
                return optional.get().substring(2);
            } else {
                return optional.get();
            }
        }*/

        Optional<String> optional = Arrays.stream(versions).filter(StringUtils::isNotBlank).reduce((first, second) -> second);
        if (optional.isPresent()) {
            return optional.get();
        }
        return "undefined";
    }


    private boolean isOperationSymbols(String version) {
        return version.startsWith(">=") || version.startsWith("<=");
    }


    public ForeignId createNameVersionForeignId(Supplier supplier, String name) {
        return createNameVersionForeignId(supplier, name, null);
    }

    public ForeignId createYoctoForeignId(String layer, String name, String version) {
        ForeignId foreignId = createNameVersionForeignId(Supplier.YOCTO, name, version);
        foreignId.setLayer(layer);
        return foreignId;
    }

    public ForeignId createYoctoForeignId(String layer, String name) {
        return createYoctoForeignId(layer, name, null);
    }

    public ForeignId createMavenForeignId(String group, String name, String version) {
        ForeignId foreignId = createNameVersionForeignId(Supplier.MAVEN, name, version);
        foreignId.setGroup(group);
        return foreignId;
    }

    public ForeignId createMavenForeignId(String group, String name) {
        return createMavenForeignId(group, name, null);
    }

    public ForeignId createArchitectureForeignId(Supplier supplier, String name, String version, String architecture) {
        ForeignId foreignId = createNameVersionForeignId(supplier, name, version);
        foreignId.setArchitecture(architecture);
        return foreignId;
    }

    public ForeignId createArchitectureForeignId(Supplier supplier, String name, String architecture) {
        return createArchitectureForeignId(supplier, name, null, architecture);
    }

    public ForeignId createConanForeignId(Supplier supplier, String name){
        return createConanForeignId(supplier,name,null);
    }
    public ForeignId createConanForeignId(Supplier supplier, String name, String version){
        ForeignId foreignId = new ForeignId(supplier);
        foreignId.setName(name);
        foreignId.setVersion(version);
        return foreignId;
    }

    public static void main(String[] args) {
        String s = "10 || 11 ||12";
        String[] split = s.split("\\|");
        System.out.println("split = " + split);

    }
}
