package com.salesforce.trellis.common;

import com.fasterxml.jackson.databind.Module;

/**
 * @author pcal
 * @since 0.0.3
 */
public interface GoldFileSerializerConfig {

    Module getJacksonModule();

}
