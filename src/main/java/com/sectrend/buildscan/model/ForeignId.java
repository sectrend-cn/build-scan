
package com.sectrend.buildscan.model;

import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.utils.Baseable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:41
 */

public class ForeignId extends Baseable {

    public static final ForeignIdFactory FACTORY = new ForeignIdFactory();
    private final Supplier supplier;

    private List<String> pieces = new ArrayList<>();

    private String prefix;

    private String suffix;


    public ForeignId(Supplier supplier) {
        this.supplier = supplier;
        this.pieces.add(0, (String)null);
        this.pieces.add(1, (String)null);
    }

    public String[] getForeignIdPieces() {
        List<String> foreignIdPieces = new ArrayList<>();
        if (StringUtils.isNotBlank(this.prefix))
            foreignIdPieces.add(this.prefix);
        this.pieces
                .stream()
                .filter(StringUtils::isNotBlank)
                .forEach(foreignIdPieces::add);
        if (StringUtils.isNotBlank(this.suffix))
            foreignIdPieces.add(this.suffix);
        return foreignIdPieces.<String>toArray(new String[0]);
    }


    public Supplier getSupplier() {
        return this.supplier;
    }

    public void setLayer(String layer) {
        this.prefix = layer;
    }

    public String getGroup() {
        return getPrefix();
    }

    public void setGroup(String group) {
        this.prefix = group;
    }

    public String getName() {
        return this.pieces.get(0);
    }

    public void setName(String name) {
        this.pieces.set(0, name);
    }

    public String getVersion() {
        return this.pieces.get(1);
    }

    public void setVersion(String version) {
        this.pieces.set(1, version);
    }

    public void setArchitecture(String architecture) {
        this.suffix = architecture;
    }

    public void setModuleNames(String[] moduleNames) {
        this.prefix = null;
        this.suffix = null;
        this.pieces.set(0, (String)null);
        this.pieces.set(1, (String)null);
        Arrays.<String>stream(moduleNames)
                .filter(StringUtils::isNotBlank)
                .forEach(this.pieces::add);
    }

    public String getPath() {
        return this.pieces.get(0);
    }

    public void setPath(String path) {
        this.prefix = null;
        this.suffix = null;
        this.pieces.set(0, path);
        this.pieces.set(1, (String)null);
    }

    protected String getPrefix() {
        return this.prefix;
    }

    public String toStringForeignIds() {
        StringBuilder sb = new StringBuilder();

        // 包名
        if (StringUtils.isNotBlank(this.prefix)) {
            sb.append(this.prefix);
        }
        if (StringUtils.isNotBlank(this.pieces.get(0))) {
            if(sb.length() > 0)
                sb.append("__");

            sb.append(this.pieces.get(0));
        }
        if (StringUtils.isNotBlank(this.pieces.get(1))) {
            sb.append("__");
            sb.append(this.pieces.get(1));
        }
        return sb.toString();
    }

}
