
package com.sectrend.buildscan.model;

import com.sectrend.buildscan.utils.Baseable;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:42
 */
public class Supplier extends Baseable {
//    public static final Supplier ALPINE = new Supplier("/", "alpine");
//
//    public static final Supplier ANACONDA = new Supplier("/", "anaconda");
//
//    public static final Supplier APACHE_SOFTWARE = new Supplier("/", "apache_software");
//
//    public static final Supplier BITBUCKET = new Supplier("/", "bitbucket");
//
//    public static final Supplier BOWER = new Supplier("/", "bower");
//
//    public static final Supplier BUSYBOX = new Supplier("/", "busybox");
//
//    public static final Supplier CRATES = new Supplier("/", "crates");
//
//    public static final Supplier CENTOS = new Supplier("/", "centos");
//
//    public static final Supplier CODEPLEX = new Supplier("/", "codeplex");
//
//    public static final Supplier CODEPLEX_GROUP = new Supplier("/", "codeplex_group");
//
//    public static final Supplier CPAN = new Supplier("/", "cpan");
//
//    public static final Supplier CRAN = new Supplier("/", "cran");
//
//    public static final Supplier CONAN = new Supplier("/", "conan");
//
//    public static final Supplier DART = new Supplier("/", "dart");
//
//    public static final Supplier DEBIAN = new Supplier("/", "debian");
//
//    public static final Supplier FEDORA = new Supplier("/", "fedora");
//
//    public static final Supplier FREEDESKTOP_ORG = new Supplier("/", "freedesktop_org");
//
//    public static final Supplier GITCAFE = new Supplier("/", "gitcafe");
//
//    public static final Supplier GITLAB = new Supplier("/", "gitlab");
//
//    public static final Supplier GITORIOUS = new Supplier("/", "gitorious");
//
//    public static final Supplier GOGET = new Supplier("/", "goget");
//
//    public static final Supplier GNU = new Supplier("/", "gnu");
//
//    public static final Supplier GOOGLECODE = new Supplier("/", "googlecode");
//
//    public static final Supplier HEX = new Supplier("/", "hex");
//
//    public static final Supplier JAVA_NET = new Supplier("/", "java_net");
//
//    public static final Supplier KDE_ORG = new Supplier("/", "kde_org");
//
//    public static final Supplier LAUNCHPAD = new Supplier("/", "launchpad");
//
//    public static final Supplier LONG_TAIL = new Supplier("/", "long_tail");
//
//    public static final Supplier NUGET = new Supplier("/", "nuget");
//
//    public static final Supplier NPMJS = new Supplier("/", "npmjs");
//
//    public static final Supplier PEAR = new Supplier("/", "pear");

    public static final Supplier PYPI = new Supplier("/", "pypi");
//
//    public static final Supplier REDHAT = new Supplier("/", "redhat");
//
//    public static final Supplier RUBYFORGE = new Supplier("/", "rubyforge");
//
//    public static final Supplier RUBYGEMS = new Supplier("/", "rubygems");
//
//    public static final Supplier SOURCEFORGE = new Supplier("/", "sourceforge");
//
//    public static final Supplier SOURCEFORGE_JP = new Supplier("/", "sourceforge_jp");
//
//    public static final Supplier UBUNTU = new Supplier("/", "ubuntu");

    public static final Supplier YOCTO = new Supplier("/", "yocto", true);

//    public static final Supplier ANDROID = new Supplier(":", "android");
//
//    public static final Supplier COCOAPODS = new Supplier(":", "cocoapods");
//
//    public static final Supplier CPE = new Supplier(":", "cpe");
//
//    public static final Supplier GITHUB = new Supplier(":", "github");

    public static final Supplier GOLANG = new Supplier(":", "golang");

    public static final Supplier MAVEN = new Supplier(":", "maven");

//    public static final Supplier PACKAGIST = new Supplier(":", "packagist");
//
//    public static final Supplier CMAKELIST = new Supplier("/", "cmakelist");
//
//    public static final Supplier MACKFILE = new Supplier("/", "mackfile");
//
//    public static final Supplier OHPM = new Supplier("/", "ohpm");
//
//    public static final Supplier PNPMJS = new Supplier("/", "pnpmjs");
//
    private final String name;

    private final String separator;

    private Boolean usePreferredNamespaceAlias;

    public Supplier(String separator, String name) {
        if (StringUtils.isBlank(name))
            throw new IllegalArgumentException("A non-blank name is required.");
        this.name = name.toLowerCase();
        this.separator = separator;
    }

    public Supplier(String separator, String name, boolean usePreferredNamespaceAlias) {
        this(separator, name);
        this.usePreferredNamespaceAlias = Boolean.valueOf(usePreferredNamespaceAlias);
    }

    public static Map<String, Supplier> getKnownSuppliers() {
        Map<String, Supplier> knownSuppliers = new HashMap<>();
        List<Field> knownStaticFinalSupplierFields = (List<Field>) Arrays.<Field>stream(Supplier.class.getFields()).filter(f -> Modifier.isStatic(f.getModifiers())).filter(f -> Modifier.isFinal(f.getModifiers())).filter(f -> f.getType().isAssignableFrom(Supplier.class)).collect(Collectors.toList());
        for (Field field : knownStaticFinalSupplierFields) {
            try {
                Supplier supplier = (Supplier) field.get(null);
                knownSuppliers.put(supplier.getName(), supplier);
            } catch (IllegalAccessException illegalAccessException) {
            }
        }
        return knownSuppliers;
    }

    public String toString() {
        return this.name;
    }

    public String getName() {
        String formatString = (this.usePreferredNamespaceAlias != null && this.usePreferredNamespaceAlias.booleanValue()) ? "@%s" : "%s";
        return String.format(formatString, new Object[]{this.name});
    }

    public String getSeparator() {
        return this.separator;
    }
}