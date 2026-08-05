package com.tacz_caliber_ammo.compat.runtime;

import java.util.Set;

public record ModRequirement(String modId, Set<String> testedVersions, boolean allowUntestedVersions) {

    public ModRequirement {
        testedVersions = Set.copyOf(testedVersions);
    }
}