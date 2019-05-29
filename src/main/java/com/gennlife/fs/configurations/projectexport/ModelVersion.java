package com.gennlife.fs.configurations.projectexport;

import com.gennlife.darren.collection.string.Matching;
import com.gennlife.darren.util.NumericVersion;
import com.gennlife.fs.common.exception.UnexpectedException;
import lombok.val;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

// http://con.ins.gennlife.cn/pages/viewpage.action?pageId=15929615
public class ModelVersion {

    public ModelVersion(@Nonnull String s) {
        s = s.trim();
        if (Matching.isEmpty(s) || !s.startsWith("V")) {
            throw new UnexpectedException();
        }
        val parts = s.split("_");
        NumericVersion customVersion = null;
        NumericVersion betaVersion = null;
        for (int i = 1; i < parts.length; ++i) {
            val part = parts[i];
            if (part.startsWith("beta.")) {
                betaVersion = new NumericVersion(part.substring("beta.".length()));
            } else if (part.startsWith("pro.")) {
                customVersion = new NumericVersion(part.substring("pro.".length()));
            } else {
                throw new UnexpectedException();
            }
        }
        _mainVersion = new NumericVersion(parts[0].substring("V".length()));
        _customVersion = customVersion;
        _betaVersion = betaVersion;
    }

    public ModelVersion(@Nonnull NumericVersion mainVersion, NumericVersion customizedVersion, NumericVersion betaVersion) {
        _mainVersion = requireNonNull(mainVersion);
        _customVersion = customizedVersion;
        _betaVersion = betaVersion;
    }

    public NumericVersion mainVersion() {
        return _mainVersion;
    }

    public NumericVersion betaVersion() {
        return _betaVersion;
    }

    public NumericVersion customVersion() {
        return _customVersion;
    }

    public boolean isRelease() {
        return _betaVersion == null;
    }

    public boolean isBeta() {
        return _betaVersion != null;
    }

    public boolean isCustomized() {
        return _customVersion != null;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("V" + _mainVersion);
        if (_customVersion != null) {
            ret.append("_pro.").append(_customVersion);
        }
        if (_betaVersion != null) {
            ret.append("_beta.").append(_betaVersion);
        }
        return ret.toString();
    }

    private final NumericVersion _mainVersion;
    private final NumericVersion _customVersion;
    private final NumericVersion _betaVersion;

}
