package scalar.scalarcalc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.*;

import scalar.Field;
import scalar.SContainer;

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

	HashMap<String, SContainer> var;

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
		type0 = new JRadioButton("実数(R)");
		type0.setMnemonic(KeyEvent.VK_R);
		type0.addActionListener(this);
		type0.setActionCommand("Real");
		type0.setSelected(true);

		type1 = new JRadioButton("分数(F)");
		type1.setMnemonic(KeyEvent.VK_F);
		type1.addActionListener(this);
		type1.setActionCommand("Frac");

		type2 = new JRadioButton("有限体(I)");
		type2.setMnemonic(KeyEvent.VK_I);
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

		button = new JButton("計算(C)");
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
				add(new JLabel("位数:"));
				add(order);
			}
		};

		// 数式入力画面
		JPanel panelFomula = new JPanel() {
			{
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				// 数式入力タブ
				add(new JLabel("数式"));
				add(fomula);
				add(new JLabel("結果"));
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
			SContainer sc = null;
			if (type0.isSelected())
				sc = Interpreter.run(text, Field.Real, var);
			else if (type1.isSelected())
				sc = Interpreter.run(text, Field.Real, var);
			else if (type2.isSelected()) {
				Field f = Field.Galois;
				f.setOrder((int) order.getValue());
				sc = Interpreter.run(text, f, var);
			}
			if (sc != null)
				answer.setText(sc.toString());
		} catch (Syntax_error e) {
			JOptionPane.showMessageDialog(this, "数式エラー　不適切なトークン" + e + "を検出しました", "エラー", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (Var_not_exist_error e) {
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
