package net.csibio.propro.algorithm.simulator;

public class Parameters2 implements Parameter {
    Instance parameter = new Instance();
    float b;

    public Parameters2() {
        /*
         * CID param sun new training
         * data:2013-6-7
         */
        String paraString =
                "     -0.2613 * AA_A_-2 +\n" +
                        "     -0.2035 * AA_C_-2 +\n" +
                        "      0.1109 * AA_D_-2 +\n" +
                        "     -0.0019 * AA_E_-2 +\n" +
                        "     -0.4231 * AA_F_-2 +\n" +
                        "     -0.1142 * AA_G_-2 +\n" +
                        "     -0.8213 * AA_H_-2 +\n" +
                        "     -0.3998 * AA_I_-2 +\n" +
                        "     -0.5093 * AA_K_-2 +\n" +
                        "     -0.4836 * AA_L_-2 +\n" +
                        "     -0.3409 * AA_M_-2 +\n" +
                        "     -0.1774 * AA_N_-2 +\n" +
                        "     -0.0565 * AA_P_-2 +\n" +
                        "     -0.2116 * AA_Q_-2 +\n" +
                        "      0.068  * AA_R_-2 +\n" +
                        "     -0.2262 * AA_S_-2 +\n" +
                        "     -0.3493 * AA_T_-2 +\n" +
                        "     -0.4004 * AA_V_-2 +\n" +
                        "     -0.5343 * AA_W_-2 +\n" +
                        "     -0.4386 * AA_Y_-2 +\n" +
                        "      0.1295 * AA_A_-1 +\n" +
                        "     -0.2827 * AA_C_-1 +\n" +
                        "     -0.0257 * AA_D_-1 +\n" +
                        "     -0.1719 * AA_E_-1 +\n" +
                        "     -0.0429 * AA_F_-1 +\n" +
                        "      1.075  * AA_G_-1 +\n" +
                        "     -0.225  * AA_H_-1 +\n" +
                        "     -0.5178 * AA_I_-1 +\n" +
                        "     -0.4538 * AA_K_-1 +\n" +
                        "     -0.1284 * AA_L_-1 +\n" +
                        "     -0.0689 * AA_M_-1 +\n" +
                        "      0.54   * AA_N_-1 +\n" +
                        "      0.6044 * AA_P_-1 +\n" +
                        "     -0.1912 * AA_Q_-1 +\n" +
                        "     -0.163  * AA_R_-1 +\n" +
                        "      0.5422 * AA_S_-1 +\n" +
                        "      0.3157 * AA_T_-1 +\n" +
                        "     -0.5149 * AA_V_-1 +\n" +
                        "     -0.2434 * AA_W_-1 +\n" +
                        "     -0.1632 * AA_Y_-1 +\n" +
                        "      0.033  * AA_A_0 +\n" +
                        "     -0.4516 * AA_C_0 +\n" +
                        "      0.1265 * AA_D_0 +\n" +
                        "      0.3143 * AA_E_0 +\n" +
                        "     -0.0657 * AA_F_0 +\n" +
                        "     -1.4509 * AA_G_0 +\n" +
                        "     -0.0477 * AA_H_0 +\n" +
                        "      0.7661 * AA_I_0 +\n" +
                        "      0.5901 * AA_K_0 +\n" +
                        "      0.5677 * AA_L_0 +\n" +
                        "      0.2703 * AA_M_0 +\n" +
                        "     -0.4312 * AA_N_0 +\n" +
                        "     -2.977  * AA_P_0 +\n" +
                        "      0.4825 * AA_Q_0 +\n" +
                        "     -0.1841 * AA_R_0 +\n" +
                        "     -0.6975 * AA_S_0 +\n" +
                        "     -0.2347 * AA_T_0 +\n" +
                        "      0.734  * AA_V_0 +\n" +
                        "     -0.045  * AA_W_0 +\n" +
                        "     -0.0759 * AA_Y_0 +\n" +
                        "     -0.0811 * AA_A_1 +\n" +
                        "     -0.1134 * AA_C_1 +\n" +
                        "     -0.266  * AA_D_1 +\n" +
                        "     -0.3349 * AA_E_1 +\n" +
                        "      0.1132 * AA_F_1 +\n" +
                        "      0.0874 * AA_G_1 +\n" +
                        "      0.318  * AA_H_1 +\n" +
                        "     -0.0766 * AA_I_1 +\n" +
                        "     -0.0651 * AA_K_1 +\n" +
                        "     -0.0572 * AA_L_1 +\n" +
                        "     -0.1998 * AA_M_1 +\n" +
                        "     -0.1982 * AA_N_1 +\n" +
                        "      0.9188 * AA_P_1 +\n" +
                        "     -0.3701 * AA_Q_1 +\n" +
                        "     -0.299  * AA_R_1 +\n" +
                        "      0.0249 * AA_S_1 +\n" +
                        "     -0.053  * AA_T_1 +\n" +
                        "     -0.0568 * AA_V_1 +\n" +
                        "      0.2101 * AA_W_1 +\n" +
                        "      0.0927 * AA_Y_1 +\n" +
                        "      0.1372 * KR_K_-8 +\n" +
                        "     -0.2761 * KR_K_-7 +\n" +
                        "     -0.134  * KR_K_-6 +\n" +
                        "     -0.1631 * KR_K_-5 +\n" +
                        "     -0.1571 * KR_K_-4 +\n" +
                        "     -0.0568 * KR_K_-3 +\n" +
                        "      0.0214 * KR_K_2 +\n" +
                        "     -0.0233 * KR_K_3 +\n" +
                        "     -0.1725 * KR_K_4 +\n" +
                        "     -0.143  * KR_K_5 +\n" +
                        "      0.4284 * KR_R_-8 +\n" +
                        "     -0.3369 * KR_R_-7 +\n" +
                        "     -0.0129 * KR_R_-6 +\n" +
                        "      0.2214 * KR_R_-5 +\n" +
                        "     -0.0871 * KR_R_-4 +\n" +
                        "     -0.2518 * KR_R_-3 +\n" +
                        "     -1.3064 * KR_R_1 +\n" +
                        "      0.0608 * KR_R_2 +\n" +
                        "      0.0575 * KR_R_3 +\n" +
                        "      0.0829 * KR_R_4 +\n" +
                        "      0.0928 * KR_R_5 +\n" +
                        "     -1.3061 * Cproton_X_2 +\n" +
                        "     -1.1668 * Cproton_X_3 +\n" +
                        "     -0.9705 * Cproton_X_4 +\n" +
                        "     -0.6184 * Cproton_X_5 +\n" +
                        "     -0.5307 * Cproton_X_6 +\n" +
                        "     -0.3882 * Cproton_X_7 +\n" +
                        "     -0.2685 * Cproton_X_8 +\n" +
                        "     -0.1789 * Cproton_X_9 +\n" +
                        "     -0.086  * Cproton_X_10 +\n" +
                        "      0.0672 * Ndis_X_1 +\n" +
                        "     -0.3234 * Ndis_X_2 +\n" +
                        "     -0.5312 * Ndis_X_3 +\n" +
                        "     -0.3271 * Ndis_X_4 +\n" +
                        "     -0.1535 * Ndis_X_5 +\n" +
                        "      2.6    * dR_A_0 +\n" +
                        "      3.1922 * dR_C_0 +\n" +
                        "      0.5351 * dR_D_0 +\n" +
                        "      1.572  * dR_E_0 +\n" +
                        "      2.8707 * dR_F_0 +\n" +
                        "      2.3093 * dR_G_0 +\n" +
                        "      1.6656 * dR_H_0 +\n" +
                        "      2.8989 * dR_I_0 +\n" +
                        "      0.6332 * dR_K_0 +\n" +
                        "      2.6612 * dR_L_0 +\n" +
                        "      1.8433 * dR_M_0 +\n" +
                        "      2.0894 * dR_N_0 +\n" +
                        "      1.4531 * dR_P_0 +\n" +
                        "      1.9341 * dR_Q_0 +\n" +
                        "      0.2137 * dR_R_0 +\n" +
                        "      2.7131 * dR_S_0 +\n" +
                        "      3.0511 * dR_T_0 +\n" +
                        "      2.8934 * dR_V_0 +\n" +
                        "      2.6386 * dR_W_0 +\n" +
                        "      2.9205 * dR_Y_0 +\n" +
                        "     -0.9191 * dR_A_1 +\n" +
                        "     -0.5242 * dR_C_1 +\n" +
                        "     -0.8647 * dR_D_1 +\n" +
                        "     -0.899  * dR_E_1 +\n" +
                        "     -0.8163 * dR_F_1 +\n" +
                        "     -0.829  * dR_G_1 +\n" +
                        "     -0.913  * dR_H_1 +\n" +
                        "     -0.9254 * dR_I_1 +\n" +
                        "     -0.8535 * dR_K_1 +\n" +
                        "     -0.937  * dR_L_1 +\n" +
                        "     -0.9958 * dR_M_1 +\n" +
                        "     -0.8179 * dR_N_1 +\n" +
                        "     -0.3917 * dR_P_1 +\n" +
                        "     -0.8845 * dR_Q_1 +\n" +
                        "     -0.8629 * dR_R_1 +\n" +
                        "     -0.9563 * dR_S_1 +\n" +
                        "     -1.0326 * dR_T_1 +\n" +
                        "     -0.922  * dR_V_1 +\n" +
                        "     -0.6409 * dR_W_1 +\n" +
                        "     -0.7496 * dR_Y_1 +\n" +
                        "      0.01   * dR_A_2 +\n" +
                        "      0.2771 * dR_C_2 +\n" +
                        "      0.058  * dR_D_2 +\n" +
                        "      0.0697 * dR_E_2 +\n" +
                        "      0.1202 * dR_F_2 +\n" +
                        "      0.1152 * dR_G_2 +\n" +
                        "      0.176  * dR_H_2 +\n" +
                        "      0.1026 * dR_I_2 +\n" +
                        "      0.2763 * dR_K_2 +\n" +
                        "      0.0744 * dR_L_2 +\n" +
                        "      0.0501 * dR_M_2 +\n" +
                        "      0.181  * dR_N_2 +\n" +
                        "      0.5263 * dR_P_2 +\n" +
                        "      0.1551 * dR_Q_2 +\n" +
                        "      0.626  * dR_R_2 +\n" +
                        "      0.0366 * dR_S_2 +\n" +
                        "      0.0959 * dR_T_2 +\n" +
                        "      0.0749 * dR_V_2 +\n" +
                        "      0.043  * dR_W_2 +\n" +
                        "      0.1558 * dR_Y_2 +\n" +
                        "      0.861 ";
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
