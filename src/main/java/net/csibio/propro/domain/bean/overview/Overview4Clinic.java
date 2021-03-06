/*
 * Copyright (c) 2020 CSi Biotech
 * Aird and AirdPro are licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package net.csibio.propro.domain.bean.overview;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;

@Data
public class Overview4Clinic {

    String id;
    String name;
    String runId;
    Boolean defaultOne;
    HashMap<String, Double> weights;
    Double minTotalScore;
    /**
     * 关于本次分析的统计数据
     */
    HashMap<String, Object> statistic = new HashMap<>();
    /**
     * 分析实验的创建时间
     */
    Date createDate;
}
