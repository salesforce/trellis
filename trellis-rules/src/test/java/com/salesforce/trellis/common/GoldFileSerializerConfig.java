/**
 * Copyright (c) 2020, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.trellis.common;

import com.fasterxml.jackson.databind.Module;

/**
 * @author pcal
 * @since 0.0.3
 */
public interface GoldFileSerializerConfig {

    Module getJacksonModule();

}
