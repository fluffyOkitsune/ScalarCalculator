package scalar.scalarcalc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.*;

import scalar.SContainer;
import scalar.field.*;

/**
 * 数式かるきゅれ～た
 *
 * @version 1.00
 * @author おきつね
 */
@SuppressWarnings("serial")

public class ScalarCalc extends JFrame implements ActionListener, ChangeListener {
	private JSpinner order;
	private JTextPane fomula;
	private JTextPane answer;
	private JButton button;

	private JRadioButton type0;
	private JRadioButton type1;
	private JRadioButton type2;

	HashMap<String, String> var;

	public static void main(String args[]) {
		new ScalarCalc() {
			{
				setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				try {
					String look = UIManager.getSystemLookAndFeelClassName();
					UIManager.setLookAndFeel(look);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}

	private ScalarCalc() {
		type0 = new JRadioButton("実数(Double)");
		type0.setMnemonic(KeyEvent.VK_D);
		type0.addActionListener(this);
		type0.setActionCommand("Real");
		type0.setSelected(true);

		type1 = new JRadioButton("有理数(Rational number)");
		type1.setMnemonic(KeyEvent.VK_R);
		type1.addActionListener(this);
		type1.setActionCommand("Frac");

		type2 = new JRadioButton("有限体(Galois Field)");
		type2.setMnemonic(KeyEvent.VK_G);
		type2.addActionListener(this);
		type2.setActionCommand("Field");

		var = new HashMap<>();

		new ButtonGroup() {
			{
				add(type0);
				add(type1);
				add(type2);
			}
		};

		button = new JButton("計算(Calculate) ");
		button.addActionListener(this);
		button.setActionCommand("Calc");
		button.setMnemonic(KeyEvent.VK_C);

		order = new JSpinner(new SpinnerNumberModel(7, 2, null, 1));
		order.setEnabled(false);

		fomula = new JTextPane();
		answer = new JTextPane();
		answer.setEditable(false);

		JPanel panelHeader = new JPanel() {
			{
				setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
				add(type0);
				add(type1);
				add(type2);
				add(button);
				add(new JLabel("位数 Order :"));
				add(order);
			}
		};

		// 数式入力画面
		JPanel panelFomula = new JPanel() {
			{
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				// 数式入力タブ
				add(new JLabel("数式 Formula"));
				add(fomula);
				add(new JLabel("結果 Result"));
				add(answer);
			}
		};

		getContentPane().add(panelFomula, BorderLayout.CENTER);
		getContentPane().add(panelHeader, BorderLayout.NORTH);

		// ウィンドウの設定
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("数式かるきゅれ～た");
		setBounds(0, 0, 480, 360);
		setPreferredSize(new Dimension(480, 360));
		setVisible(true);

	}

	// 数式計算モード
	void fomulaCalc() {
		try {
			String text = fomula.getText();
			SContainer<? extends Comparable<?>> sc = null;

			if (type0.isSelected()) { // 浮動小数点
				Field<Double> field = new DoubleField();
				HashMap<String, SContainer<Double>> varName = loadVarTable(var, field);
				sc = Interpreter.run(text, field, varName);
				saveVarTable(varName, field);

			} else if (type1.isSelected()) { // 有理整数環
				Field<Frac> field = new RationalField();
				HashMap<String, SContainer<Frac>> varName = loadVarTable(var, field);
				sc = Interpreter.run(text, field, loadVarTable(var, field));
				saveVarTable(varName, field);

			} else if (type2.isSelected()) { // ガロア体
				Field<BigInteger> field = new GaloisField((int) order.getValue());
				HashMap<String, SContainer<BigInteger>> varName = loadVarTable(var, field);
				sc = Interpreter.run(text, field, loadVarTable(var, field));
				saveVarTable(varName, field);
			}

			if (sc != null)
				answer.setText(sc.toString());

		} catch (SyntaxErrorException e) {
			JOptionPane.showMessageDialog(this, "数式エラー　不適切なトークン" + e + "を検出しました", "エラー", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (VarNotExistException e) {
			JOptionPane.showMessageDialog(this, "変数" + e + "は存在しないか、定義されていません", "エラー", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (ClassCastException e) {
			JOptionPane.showMessageDialog(this, e, "エラー", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(this, e, "エラー", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (ArithmeticException e) {
			JOptionPane.showMessageDialog(this, e, "エラー", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	private <E extends Comparable<E>> HashMap<String, SContainer<E>> loadVarTable(HashMap<String, String> hashMap,
			Field<E> field) {
		HashMap<String, SContainer<E>> res = new HashMap<>();
		Set<String> key = hashMap.keySet();

		for (String k : key)
			res.put(k, SContainer.make(hashMap.get(k), field));
		return res;
	}

	private <E extends Comparable<E>> HashMap<String, String> saveVarTable(HashMap<String, SContainer<E>> hashMap,
			Field<E> field) {
		HashMap<String, String> res = new HashMap<>();
		Set<String> key = hashMap.keySet();

		for (String k : key)
			res.put(k, hashMap.get(k).toString());
		return res;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		// 行列タイプの選択
		case "Real":
		case "Frac":
			order.setEnabled(false);
			break;
		case "Field":
			order.setEnabled(true);
			break;
		// 計算の実行
		case "Calc":
			fomulaCalc();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
	}
}
