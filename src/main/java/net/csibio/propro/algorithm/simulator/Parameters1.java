package net.csibio.propro.algorithm.simulator;

public class Parameters1 implements Parameter {
    Instance parameter = new Instance();
    float b;

    public Parameters1() {

        String paraString =
                "     -0.6455 * AA_A_-2 +\n" +
                        "     -0.5673 * AA_C_-2 +\n" +
                        "     -0.3539 * AA_D_-2 +\n" +
                        "     -0.4754 * AA_E_-2 +\n" +
                        "     -0.7519 * AA_F_-2 +\n" +
                        "     -0.5311 * AA_G_-2 +\n" +
                        "     -1.0194 * AA_H_-2 +\n" +
                        "     -0.7687 * AA_I_-2 +\n" +
                        "     -0.9243 * AA_K_-2 +\n" +
                        "     -0.8151 * AA_L_-2 +\n" +
                        "     -0.5296 * AA_M_-2 +\n" +
                        "     -0.5088 * AA_N_-2 +\n" +
                        "     -0.6773 * AA_P_-2 +\n" +
                        "     -0.5952 * AA_Q_-2 +\n" +
                        "      0.0904 * AA_R_-2 +\n" +
                        "     -0.5621 * AA_S_-2 +\n" +
                        "     -0.6772 * AA_T_-2 +\n" +
                        "     -0.7205 * AA_V_-2 +\n" +
                        "     -0.828  * AA_W_-2 +\n" +
                        "     -0.7677 * AA_Y_-2 +\n" +
                        "      2.4569 * AA_A_-1 +\n" +
                        "      2.4415 * AA_C_-1 +\n" +
                        "      2.0824 * AA_D_-1 +\n" +
                        "      2.1268 * AA_E_-1 +\n" +
                        "      2.3151 * AA_F_-1 +\n" +
                        "      3.3823 * AA_G_-1 +\n" +
                        "      2.0957 * AA_H_-1 +\n" +
                        "      1.8422 * AA_I_-1 +\n" +
                        "      1.928  * AA_K_-1 +\n" +
                        "      2.1555 * AA_L_-1 +\n" +
                        "      2.2503 * AA_M_-1 +\n" +
                        "      2.7324 * AA_N_-1 +\n" +
                        "      2.9318 * AA_P_-1 +\n" +
                        "      2.1635 * AA_Q_-1 +\n" +
                        "      2.8486 * AA_R_-1 +\n" +
                        "      2.8306 * AA_S_-1 +\n" +
                        "      2.5748 * AA_T_-1 +\n" +
                        "      1.8755 * AA_V_-1 +\n" +
                        "      2.135  * AA_W_-1 +\n" +
                        "      2.2698 * AA_Y_-1 +\n" +
                        "     -0.1272 * AA_A_0 +\n" +
                        "     -0.5628 * AA_C_0 +\n" +
                        "      0.2183 * AA_D_0 +\n" +
                        "      0.3341 * AA_E_0 +\n" +
                        "     -0.132  * AA_F_0 +\n" +
                        "     -1.4261 * AA_G_0 +\n" +
                        "     -0.4273 * AA_H_0 +\n" +
                        "      0.6874 * AA_I_0 +\n" +
                        "     -0.0025 * AA_K_0 +\n" +
                        "      0.4143 * AA_L_0 +\n" +
                        "      0.2162 * AA_M_0 +\n" +
                        "     -0.4453 * AA_N_0 +\n" +
                        "     -2.2844 * AA_P_0 +\n" +
                        "      0.3816 * AA_Q_0 +\n" +
                        "     -0.4882 * AA_R_0 +\n" +
                        "     -0.7963 * AA_S_0 +\n" +
                        "     -0.3395 * AA_T_0 +\n" +
                        "      0.6064 * AA_V_0 +\n" +
                        "     -0.0439 * AA_W_0 +\n" +
                        "     -0.1149 * AA_Y_0 +\n" +
                        "      0.5015 * AA_A_1 +\n" +
                        "      0.5034 * AA_C_1 +\n" +
                        "      0.2679 * AA_D_1 +\n" +
                        "      0.1198 * AA_E_1 +\n" +
                        "      0.6674 * AA_F_1 +\n" +
                        "      0.569  * AA_G_1 +\n" +
                        "      0.9483 * AA_H_1 +\n" +
                        "      0.4521 * AA_I_1 +\n" +
                        "      0.5249 * AA_K_1 +\n" +
                        "      0.4806 * AA_L_1 +\n" +
                        "      0.361  * AA_M_1 +\n" +
                        "      0.3353 * AA_N_1 +\n" +
                        "      1.8097 * AA_P_1 +\n" +
                        "      0.0779 * AA_Q_1 +\n" +
                        "     -0.3322 * AA_R_1 +\n" +
                        "      0.5844 * AA_S_1 +\n" +
                        "      0.5468 * AA_T_1 +\n" +
                        "      0.4481 * AA_V_1 +\n" +
                        "      0.8165 * AA_W_1 +\n" +
                        "      0.6841 * AA_Y_1 +\n" +
                        "      0.112  * KR_K_-8 +\n" +
                        "      0.1587 * KR_K_-7 +\n" +
                        "      0.1185 * KR_K_-6 +\n" +
                        "      0.1379 * KR_K_-5 +\n" +
                        "      0.0584 * KR_K_-4 +\n" +
                        "      0.0124 * KR_K_-3 +\n" +
                        "     -0.2618 * KR_K_1 +\n" +
                        "     -0.3219 * KR_K_2 +\n" +
                        "     -0.1518 * KR_K_3 +\n" +
                        "     -0.1092 * KR_K_4 +\n" +
                        "     -0.2399 * KR_K_5 +\n" +
                        "      0.3307 * KR_R_-8 +\n" +
                        "      0.5328 * KR_R_-7 +\n" +
                        "      0.5083 * KR_R_-6 +\n" +
                        "      0.6305 * KR_R_-5 +\n" +
                        "      0.5179 * KR_R_-4 +\n" +
                        "      0.5612 * KR_R_-3 +\n" +
                        "      0.596  * KR_R_1 +\n" +
                        "     -0.8729 * KR_R_2 +\n" +
                        "     -0.5796 * KR_R_3 +\n" +
                        "     -0.3581 * KR_R_4 +\n" +
                        "     -0.2496 * KR_R_5 +\n" +
                        "      0.2575 * Cproton_X_2 +\n" +
                        "     -0.0154 * Cproton_X_3 +\n" +
                        "     -0.3491 * Cproton_X_4 +\n" +
                        "     -0.3733 * Cproton_X_5 +\n" +
                        "     -0.3151 * Cproton_X_6 +\n" +
                        "     -0.4226 * Cproton_X_7 +\n" +
                        "     -0.3429 * Cproton_X_8 +\n" +
                        "     -0.1989 * Cproton_X_9 +\n" +
                        "     -0.0836 * Cproton_X_10 +\n" +
                        "     -0.2408 * Ndis_X_1 +\n" +
                        "     -0.5125 * Ndis_X_2 +\n" +
                        "     -0.492  * Ndis_X_3 +\n" +
                        "     -0.2981 * Ndis_X_4 +\n" +
                        "     -0.2683 * Ndis_X_5 +\n" +
                        "     -0.3441 * Ndis_X_6 +\n" +
                        "      1.6237 * dR_A_0 +\n" +
                        "      1.1852 * dR_C_0 +\n" +
                        "      1.6736 * dR_D_0 +\n" +
                        "      1.8474 * dR_E_0 +\n" +
                        "      1.056  * dR_F_0 +\n" +
                        "      1.5304 * dR_G_0 +\n" +
                        "      0.2219 * dR_H_0 +\n" +
                        "      1.0451 * dR_I_0 +\n" +
                        "     -0.1379 * dR_K_0 +\n" +
                        "      1.0192 * dR_L_0 +\n" +
                        "      0.8154 * dR_M_0 +\n" +
                        "      1.3586 * dR_N_0 +\n" +
                        "     -0.5509 * dR_P_0 +\n" +
                        "      1.3564 * dR_Q_0 +\n" +
                        "     -1.2209 * dR_R_0 +\n" +
                        "      1.6935 * dR_S_0 +\n" +
                        "      1.6271 * dR_T_0 +\n" +
                        "      1.2569 * dR_V_0 +\n" +
                        "      0.495  * dR_W_0 +\n" +
                        "      0.9816 * dR_Y_0 +\n" +
                        "     -0.5396 * dR_A_1 +\n" +
                        "     -0.625  * dR_C_1 +\n" +
                        "     -0.5961 * dR_D_1 +\n" +
                        "     -0.5954 * dR_E_1 +\n" +
                        "     -0.4637 * dR_F_1 +\n" +
                        "     -0.6926 * dR_G_1 +\n" +
                        "     -1.006  * dR_H_1 +\n" +
                        "     -0.5028 * dR_I_1 +\n" +
                        "     -0.8058 * dR_K_1 +\n" +
                        "     -0.4828 * dR_L_1 +\n" +
                        "     -0.5611 * dR_M_1 +\n" +
                        "     -0.5362 * dR_N_1 +\n" +
                        "     -0.5765 * dR_P_1 +\n" +
                        "     -0.5331 * dR_Q_1 +\n" +
                        "     -1.477  * dR_R_1 +\n" +
                        "     -0.621  * dR_S_1 +\n" +
                        "     -0.5503 * dR_T_1 +\n" +
                        "     -0.4985 * dR_V_1 +\n" +
                        "     -0.4596 * dR_W_1 +\n" +
                        "     -0.4466 * dR_Y_1 +\n" +
                        "      0.1119 * dR_A_2 +\n" +
                        "      0.2666 * dR_C_2 +\n" +
                        "     -0.0057 * dR_D_2 +\n" +
                        "      0.0604 * dR_E_2 +\n" +
                        "      0.2335 * dR_F_2 +\n" +
                        "      0.1159 * dR_G_2 +\n" +
                        "     -0.0041 * dR_H_2 +\n" +
                        "      0.1842 * dR_I_2 +\n" +
                        "      0.0303 * dR_K_2 +\n" +
                        "      0.1802 * dR_L_2 +\n" +
                        "      0.2016 * dR_M_2 +\n" +
                        "      0.1662 * dR_N_2 +\n" +
                        "      0.5315 * dR_P_2 +\n" +
                        "      0.2205 * dR_Q_2 +\n" +
                        "      0.0416 * dR_R_2 +\n" +
                        "      0.0833 * dR_S_2 +\n" +
                        "      0.1908 * dR_T_2 +\n" +
                        "      0.1553 * dR_V_2 +\n" +
                        "      0.3146 * dR_W_2 +\n" +
                        "      0.2271 * dR_Y_2 +\n" +
                        "     -1.4893";
        String[] lines = paraString.split("\n");

        for (int i = 0; i < lines.length; i++) {
            /*
             * 0.9268 * dR_T_1 +
             */
            String per = lines[i];
            if (per.length() < 1 || per.startsWith("%")) {
                continue;
            }
            // System.out.println(per);
            per = per.replace("+", "");
            per = per.replace(" ", "");
            // System.out.println(per);
            String[] split = per.split("\\*");
            if (split.length == 1) {
                b = Float.parseFloat(per);
                continue;
            }

            float weight = Float.parseFloat(split[0]);
            String[] labels = split[1].split("_");
            String type = labels[0];
            char aa = labels[1].charAt(0);
            // System.out.println(per);
            int pos = Integer.parseInt(labels[2]);
            if (type.equals("AA")) {
                parameter.AA[pos - SimuConst.AAbegin][SimuConst
                        .getIndex(aa)] = weight;
            }
            if (type.equals("KR")) {
                // System.out.println(pos+"\t"+aa);
                if (aa == 'K')
                    parameter.KR[0][pos - SimuConst.KRbegin] = weight;
                if (aa == 'R')
                    parameter.KR[1][pos - SimuConst.KRbegin] = weight;
            }

            if (type.equals("Cproton")) {
                parameter.protondis[pos] = weight;
            }

            if (type.equals("Ndis")) {
                parameter.Ndis[pos] = weight;
            }
            if (type.equals("dR")) {
                parameter.dickA[pos][SimuConst.getIndex(aa)] = weight;
            }
        }

    }

    public float getlnR(Instance in) {
        float lnR = b;
        lnR += SunArray.getInnerProduct(this.parameter.AA, in.AA);
        lnR += SunArray.getInnerProduct(this.parameter.KR, in.KR);

        lnR += SunArray.getInnerProduct(this.parameter.Ndis, in.Ndis);
        lnR += SunArray.getInnerProduct(this.parameter.protondis, in.protondis);
        lnR += SunArray.getInnerProduct(this.parameter.dickA, in.dickA);
        return lnR;

    }

    void print() {
        System.out.println("weightsAA................");
        SunArray.print(parameter.AA);
        System.out.println("ndisA................");
        SunArray.print(parameter.Ndis);
        System.out.println("cdisA................");
        SunArray.print(parameter.protondis);
        System.out.println("krA................");
        SunArray.print(parameter.dickA);
    }

}
