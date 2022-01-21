/*
 *  DayhoffModel.java Copyright (C) 2022 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Created on Jul 28, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package splitstree6.models.proteinModels;

/**
 * @author bryant
 * <p/>
 * Dayhoff model
 */
public class DayhoffModel extends ProteinModel {

	public static final double[] dvals =
			{-2.2090474e+00,
					-1.9332190e+00,
					-1.7483601e+00,
					-1.6485457e+00,
					-1.5450560e+00,
					-1.3385941e+00,
					-1.2978620e+00,
					-1.1050579e+00,
					-1.0432332e+00,
					-8.7963587e-01,
					-8.0873558e-01,
					-7.7685595e-01,
					-7.2505029e-01,
					-7.1124956e-01,
					-5.4171967e-01,
					-4.3042371e-01,
					-2.8965813e-01,
					-2.6695132e-01,
					-2.3554833e-01,
					7.7330722e-17};
	public static final double[][] V =
			{{5.5611096e-02, -2.7072411e-01, 3.2756969e-02, -5.8347142e-01, -5.2627744e-01, -1.2437110e-01, 1.0367774e-01, -2.1965910e-01, -2.2089746e-01, 6.4909361e-02, 3.3384406e-02, -4.8565308e-02, -1.0097077e-01, 9.0339022e-02, 2.2733704e-01, 1.5391550e-02, 9.2610646e-02, -6.1640654e-02, 6.6393792e-02, -2.9517268e-01},
					{3.5767530e-02, -3.4059011e-02, -3.4145703e-02, 1.8480289e-02, -5.7808905e-03, 1.3556095e-01, -5.8459343e-02, -6.2300446e-01, 3.7320869e-01, -2.6863017e-01, -6.3987984e-02, 6.2243832e-02, 4.7036245e-02, 2.7893745e-01, -4.6682500e-01, 9.5683726e-02, 1.1164852e-01, -7.4629115e-02, -2.5238656e-02, -2.0224727e-01},
					{6.8522229e-01, -1.9186495e-01, 5.0271078e-01, 1.4745078e-01, 1.0616812e-01, 2.3005700e-01, -2.0888592e-01, 3.2305427e-02, -1.5612322e-01, 6.2132689e-02, 2.9494592e-02, -2.1356327e-02, 4.7462817e-02, -1.3117718e-01, -2.3880307e-02, 9.4765164e-02, 7.6146048e-02, -6.4534382e-02, 3.6835279e-02, -2.0107700e-01},
					{-5.3727985e-01, -3.6706882e-01, 5.4496371e-02, 9.4693397e-02, 3.5113230e-03, 3.4737934e-01, -2.4970686e-01, -4.5796574e-02, 1.5087661e-01, 3.1577984e-01, 1.3934647e-01, -3.3009611e-02, 1.0118694e-01, -3.7088831e-01, 4.0786638e-02, 1.2392605e-01, 1.1435250e-01, -9.2215330e-02, 5.7101534e-02, -2.1649931e-01},
					{1.2599117e-02, -2.6757365e-02, -2.0665852e-02, 2.4761902e-03, 7.6260683e-04, -1.0023332e-03, 2.1427989e-03, 2.2855549e-02, 2.4514778e-02, 1.1229102e-02, 1.1032241e-02, 5.2151796e-02, 3.3594882e-02, -3.3118407e-02, -5.5840149e-02, -6.5567768e-02, 2.4489421e-01, 9.3775386e-01, 1.1010364e-01, -1.8295892e-01},
					{-7.9619723e-02, -1.9509461e-01, 2.9884428e-01, 1.2577591e-01, 1.6335186e-01, -6.1754268e-01, 4.6830837e-01, -1.7848973e-03, 1.7327550e-01, -6.3067710e-02, 4.0514317e-02, 4.1107677e-02, -6.1709104e-02, -3.2300214e-01, -1.4133557e-01, 7.2922263e-02, 7.8053391e-02, -8.1883670e-02, 3.5991682e-02, -1.9558876e-01},
					{3.3501949e-01, 4.8867547e-01, -4.1061079e-01, -1.0248124e-01, -5.7974837e-02, -2.8827456e-02, 4.3146582e-02, -6.3266656e-02, 2.7693509e-01, 3.1845276e-01, 1.5154908e-01, -3.0814191e-02, 5.7062811e-02, -4.0974114e-01, 1.9098285e-02, 1.1109788e-01, 1.0956985e-01, -9.5379791e-02, 5.7376904e-02, -2.2255325e-01},
					{1.0834923e-02, -2.9196116e-02, -9.5758082e-02, 7.1465514e-02, 1.1024353e-01, -1.8361067e-02, 1.2863951e-02, 1.6399211e-01, 2.1784445e-01, -3.0061444e-01, -2.6298786e-01, 1.8758990e-01, 4.6300493e-01, 1.8145767e-01, 5.5388056e-01, 1.2983172e-01, 1.6248827e-01, -9.5896831e-02, 8.8740100e-02, -2.9767753e-01},
					{-7.6697799e-02, 7.6658522e-02, -1.7637594e-01, -8.5631595e-02, -7.1506178e-02, 1.5152569e-01, -9.6061543e-02, 1.8713218e-01, -3.1210905e-01, -6.7949957e-01, -4.2117235e-02, 4.2265205e-02, -1.5010699e-01, -4.4602568e-01, -2.0422773e-01, 1.1814383e-01, 3.4684795e-02, -5.0999982e-02, 1.5078032e-02, -1.8335476e-01},
					{-2.6618412e-02, -6.8018013e-03, 4.5340493e-02, -5.3512187e-01, 6.1297204e-01, 3.9518379e-02, -8.3604379e-02, 9.2056605e-02, 1.4704364e-01, -1.1478551e-01, 2.6384696e-01, -3.0552572e-01, -2.8727344e-02, 8.2303439e-02, 2.3644236e-02, -2.4754299e-01, -1.1783824e-01, -7.9557425e-03, 6.6299496e-03, -1.9205719e-01},
					{-4.8114912e-03, 6.4637058e-03, -1.6089456e-02, 1.8245002e-02, -2.7412166e-02, -1.0080823e-01, -1.7794764e-01, -8.0861995e-02, -5.2876552e-02, 1.2977538e-01, -4.0794457e-01, 2.7410228e-01, 1.0627176e-01, -1.8382188e-01, -1.0164986e-01, -6.5770489e-01, -3.3523172e-01, -3.4905930e-02, -4.5704831e-02, -2.9215906e-01},
					{-6.9646826e-02, 2.4398250e-02, -7.2374981e-02, -6.3342975e-02, -8.3728891e-02, -1.3386023e-01, -7.7505055e-02, 5.2252632e-01, -1.3781815e-01, 2.3114832e-01, -1.7834259e-02, -1.7194123e-02, 2.5068174e-01, 3.4539963e-01, -5.4013712e-01, 1.2764983e-01, 1.4114954e-01, -1.2723477e-01, 3.8974072e-02, -2.8369159e-01},
					{1.1101374e-02, -6.3031062e-03, 9.5983774e-03, 2.3909320e-02, -2.3198616e-02, 5.7993014e-01, 7.6641337e-01, 1.0612707e-01, -1.6706508e-02, 4.5522894e-02, -3.9208717e-02, -5.8689949e-03, 5.0071080e-02, 3.2396981e-02, -7.3608026e-02, -1.7459194e-01, -6.9687647e-02, -2.5960162e-02, 1.7699049e-04, -1.2146187e-01},
					{5.4441612e-03, -7.9391706e-03, -6.8525007e-03, 3.5441654e-02, -4.1100824e-02, -1.0121797e-02, 5.1457151e-03, 8.9199360e-03, -1.5804805e-02, -3.1573603e-02, 5.1640931e-01, 4.7581048e-01, 6.7064967e-02, 9.9148286e-02, 3.5090472e-02, 2.4711020e-01, -5.9441796e-01, 1.2014299e-01, -1.3499527e-01, -1.9942909e-01},
					{1.7237167e-02, -3.0559209e-02, -5.8532528e-02, 6.3463296e-02, 7.6903092e-02, 8.1968438e-02, -6.5636412e-02, 2.3276719e-01, 2.4303230e-01, 9.5260315e-02, -1.5712312e-01, 2.8522774e-01, -7.9244292e-01, 1.9597654e-01, 1.0389055e-01, 4.1737784e-02, 9.9470096e-02, -6.1240849e-02, 5.2720258e-02, -2.2512208e-01},
					{-3.2280794e-01, 6.5133917e-01, 4.5493153e-01, 5.4753725e-02, 7.0269807e-02, 2.6164568e-02, -3.5305192e-03, -2.0988371e-01, -3.0184877e-01, 6.3092994e-02, -4.1318389e-03, 1.0292320e-02, -5.5094357e-02, 1.0570417e-01, 1.2037001e-01, 6.6270341e-02, 1.0673904e-01, -2.2718670e-02, 4.5971963e-02, -2.6377439e-01},
					{4.8087831e-02, -2.0214003e-01, -4.7378757e-01, 3.3968712e-01, 3.4096800e-01, -1.0047180e-01, 7.6912684e-02, -2.8030823e-01, -5.2041095e-01, 1.0940371e-01, 8.8411825e-02, -1.3349437e-01, -8.0129100e-02, 1.3233304e-01, 8.4063770e-02, -2.5517466e-04, 6.0076577e-02, -4.8637717e-02, 4.6610823e-02, -2.4195442e-01},
					{1.1693469e-03, -6.5407892e-03, -5.8112559e-03, -2.0199015e-03, -2.7224514e-04, -5.7786580e-03, 5.8225258e-03, 3.4640301e-02, -8.4890424e-03, 1.6963267e-02, 9.9106137e-03, -1.6907685e-02, -4.3815953e-03, -2.0125034e-02, 6.2358886e-02, -3.0925598e-02, 2.4086454e-01, 3.0575885e-02, -9.6082929e-01, -1.0244018e-01},
					{-1.1887847e-02, -1.3158996e-03, -2.4847644e-03, -6.3578784e-03, 7.3205555e-03, -7.3252533e-03, 1.5577204e-02, -6.5059464e-03, 6.2789805e-02, 8.1760759e-02, -4.8568514e-01, -4.6561809e-01, -6.4085203e-02, -2.9495103e-02, 1.0191507e-02, 4.4654577e-01, -5.0856098e-01, 1.7451515e-01, -1.1229798e-01, -1.7296234e-01},
					{-6.0372554e-04, 5.3089359e-02, 2.9386649e-02, 4.1803949e-01, -3.8924732e-01, -5.8474405e-02, -8.1753320e-02, 1.2883389e-01, 2.0888904e-01, -2.0959387e-01, 3.2520656e-01, -4.8817473e-01, -8.0557111e-02, 1.2236823e-01, 1.0284197e-01, -3.2451340e-01, -9.2094938e-02, -1.9481438e-02, 2.7931530e-02, -2.5439720e-01},
			};


	public static final double[] f =
			{0.087127,
					0.040904,
					0.040432,
					0.046872,
					0.033474,
					0.038255,
					0.049530,
					0.088612,
					0.033619,
					0.036886,
					0.085357,
					0.080481,
					0.014753,
					0.039772,
					0.050680,
					0.069577,
					0.058542,
					0.010494,
					0.029916,
					0.064718};

	/**
	 * constructor
	 */
	public DayhoffModel() {

		//The following vector and matrix were computed using the Q matrix specified
		//Kosiol,C. and Goldman,N. The Different Versions of the Dayhoff Rate Matrix.
		//http://www.ebi.ac.uk/goldman-srv/dayhoff

		//These are an eigenvalue decomposition
		//     V'DV = Pi^(1/2) Q Pi(-1/2)


		this.evals = dvals;

		this.evecs = V;


		this.freqs = f;

		init();
	}
}
