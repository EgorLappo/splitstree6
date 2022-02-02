/*
 * RhodopsinModel.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 */
package splitstree6.models.proteinModels;

/**
 * RhodopsinModel
 * Dave Bryant, 2005?
 */
public class RhodopsinModel extends ProteinModel {


	public RhodopsinModel() {

		/*
		 * Protein evolution matrix based on
		 *    B. Chang's data of 54 vertebrate rhodopsin proteins
		 *
		 * The matrix was recovered from the .dat files circulated with PAML 3.13d
		 */

		/* For efficiency, we precompute the eigenvalue decomposition
		 *     V'DV = Pi^(1/2) Q Pi(-1/2)
		 * We used matlab for this.
		 */

		this.evals = new double[]{
				-2.87083,
				-1.75173,
				-1.71177,
				-1.58566,
				-1.44923,
				-1.26357,
				-1.24243,
				-1.00096,
				-0.961844,
				-0.795261,
				-0.647938,
				-0.558028,
				-0.523804,
				-0.450278,
				-0.429815,
				-0.358714,
				-0.304457,
				-0.241806,
				-0.192156,
				-2.07695e-16};

		this.evecs = new double[][]{{-0.0708099, -0.0941494, 0.517871, 0.256204, 0.564913, -0.0155924, 0.00311945, 0.0215045, -0.105812, 0.401024, -0.109546, 0.00460132, -0.00822953, -0.0605362, -0.14864, -0.00992103, 0.18732, 0.00865089, 0.0503115, 0.29119},
				{0.000352242, -0.000749946, -0.00478929, -0.0127958, -0.0162917, -0.202898, -0.0944555, 0.279276, -0.615373, -0.0975483, 0.275503, -0.0603498, 0.0716545, 0.0945043, -0.0501423, 0.336488, -0.120563, -0.428454, -0.215661, 0.171164},
				{0.000142768, 0.214238, 0.0829355, 0.0404898, -0.02057, -0.274966, -0.122206, -0.00774425, 0.175199, -0.0848431, 0.171363, 0.468814, -0.384639, -0.472459, 0.203713, -0.204608, -0.0471478, -0.259729, -0.0825212, 0.19104},
				{-4.48532e-06, -0.931939, -0.0952826, -0.0466522, -0.0646497, -0.131616, -0.0581783, -0.0145772, 0.0636385, -0.0185831, 0.0876859, 0.0371548, -0.0279817, -0.012418, 0.0365999, -0.231426, 0.0461986, -0.101067, -0.0573416, 0.10099},
				{-0.0171748, 0.00368204, 0.027433, -0.000412643, 0.0111144, -0.000396256, -0.0415229, -0.0522905, 0.0145532, -0.255686, 0.0614119, -0.604317, -0.692939, 0.0264036, -0.00954238, 0.0739586, 0.150997, 0.064819, 0.105728, 0.187607},
				{0.00215174, 1.55488e-05, -0.0305411, -0.0242811, -0.045686, -0.15755, -0.0696825, 0.0222348, 0.0327603, -0.190302, -0.891145, 0.0055553, -0.0120245, 0.0746632, 0.023328, 0.0504028, -0.0239568, -0.257173, -0.168948, 0.175205},
				{0.00260928, 0.229476, 0.0240388, 0.00852423, -0.00777998, 0.0466403, 0.0450312, 0.0158654, -0.0384651, -0.0246717, 0.119875, -0.12815, 0.10341, 0.46468, 0.133019, -0.721244, 0.161415, -0.189017, -0.216796, 0.189727},
				{-0.00294678, 0.0662463, -0.025185, -0.0122294, -0.112231, 0.0103565, 0.0406627, -0.0299095, 0.0700037, -0.357047, 0.0650416, -0.11571, 0.354528, -0.396913, -0.608064, -0.119539, 0.310344, -0.0540883, 0.0486842, 0.246361},
				{-0.00160042, -0.0971166, -0.0532612, 0.0328235, 0.0759104, 0.765017, 0.413374, -0.00104882, -0.205376, -0.152227, -0.0661797, 0.137073, -0.125312, -0.205293, 0.129171, -0.0229742, -0.0947659, -0.173162, -0.0742494, 0.129222},
				{-0.763773, 0.0221745, -0.231329, 0.061802, 0.00633003, -0.203339, 0.326163, -0.121347, -0.0291578, -0.0724958, 0.0329228, 0.147507, 0.0427293, 0.14171, 0.116081, 0.114206, 0.0851486, 0.122528, 0.157172, 0.268315},
				{0.0861956, -0.0105848, 0.356599, -0.718393, 0.0306712, 0.0874388, -0.1316, -0.166027, -0.0297997, -0.201426, 0.0486957, 0.206789, 0.0802438, 0.174282, 0.13182, 0.125266, 0.0549114, 0.141001, 0.166658, 0.297643},
				{-0.00238682, 0.0118564, 0.00824887, 0.0119539, 0.028136, 0.120905, 0.0598535, -0.256298, 0.608688, 0.151255, 0.197007, -0.109721, 0.0812542, 0.199231, -0.0777338, 0.355269, -0.109158, -0.448849, -0.219772, 0.174634},
				{0.018573, -0.00405035, -0.166781, 0.451337, -0.0190949, 0.267512, -0.640978, -0.334824, -0.111655, -0.166893, 0.0310738, 0.170629, 0.0743268, 0.141668, 0.0823555, 0.0817811, 0.0581899, 0.0847312, 0.113598, 0.200739},
				{0.00954626, -0.0137604, 0.0268105, 0.140437, -0.0317098, 0.117448, -0.0992784, 0.765115, 0.342482, -0.123442, 0.0364342, -0.0496026, 0.152829, 0.0348611, 0.20812, 0.0580526, -0.110845, 0.12911, 0.207603, 0.299318},
				{-0.00254404, -0.00326082, -0.0132985, 0.00649488, 0.00755356, -0.0103581, -0.00636074, 0.000333318, 0.023582, -0.0282, 0.0398546, -0.00249166, -0.00744621, -0.0749307, -0.0250787, 0.0718138, -0.113216, 0.560963, -0.778244, 0.228024},
				{0.0162277, 0.0908275, -0.618931, -0.373749, 0.175214, 0.116867, -0.221696, 0.128396, -0.0368861, 0.490993, -0.0535599, -0.0211775, -0.0957942, -0.12043, -0.0982115, -0.0387325, 0.145139, -0.0248365, 0.0296217, 0.235785},
				{0.00411878, 0.00455863, 0.234135, 0.0834267, -0.785315, 0.090157, 0.056961, -0.00763162, -0.105969, 0.450241, -0.0624825, 0.0214105, -0.0883055, -0.0101858, -0.0341737, 0.0499615, 0.135427, 0.0323509, 0.046656, 0.242062},
				{0.000540235, -0.000191844, -0.0156094, 0.00866134, -0.00789317, -0.00419077, 0.0201404, 0.00207065, -0.0159739, 0.00500702, -0.0102822, 0.178352, -0.229995, 0.245871, -0.559707, -0.190992, -0.662791, 0.0810018, 0.179288, 0.160927},
				{0.000944272, 0.028039, 0.0124698, -0.0223345, -0.00353654, -0.102142, -0.014798, -0.279965, -0.0923525, 0.108864, -0.0210265, -0.451195, 0.315564, -0.362857, 0.326158, -0.150116, -0.498361, 0.004563, 0.146547, 0.234722},
				{0.634949, 0.01474, -0.249693, 0.19429, 0.0684208, -0.268316, 0.436531, -0.128369, -0.0399975, -0.0317174, 0.0261657, 0.130028, 0.0203872, 0.135552, 0.0985554, 0.122714, 0.116459, 0.128711, 0.164729, 0.30297}};

		this.freqs = new double[]{
				0.0848,
				0.0293,
				0.0365,
				0.0102,
				0.0352,
				0.0307,
				0.036,
				0.0607,
				0.0167,
				0.072,
				0.0886,
				0.0305,
				0.0403,
				0.0896,
				0.052,
				0.0556,
				0.0586,
				0.0259,
				0.0551,
				0.0918};
		init();
	}
}

