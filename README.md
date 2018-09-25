# ScalarCalculator
開発中です。

## Abstract
打ち込んだ数式を解釈して計算してくれるプログラム。
数字だけでなく文字(変数)も利用可能。

## How to use
SContainer.make(String s)<BR>
文字式や数式のインスタンスを生成

SContainer.print()<BR>
内容を印字

Example<BR>
% public static void main(String[] args){<BR>
% 	SContainer.make("1+1").print();<BR>
%}<BR>

## Details
###SContainerの種類
SContainer(以下コンテナ)は以下の二種類ある

1.値(v:value)を表現するコンテナ。数であるint値を持つものと、変数名であるStringを持つものの二種類ある。数と変数を同時に持つことはできない。

2.SContainer(c:container)、項(a:added)、因数(m:multipled)、指数(e:exponent)をもち「式」を表現するコンテナ。各パラメータはコンテナで、式を表現するものは((c^e)*m)+aを表現する。

a、m、eはnullポインタが許されており、aのnullポインタはaが"0"であることを、m,eのそれはそれぞれが"1"であることをそれぞれ意味する。

###SContainerによる表現

式を表すコンテナのパラメータに式を表すコンテナを代入することで項や積の連なりを表現できる。

((c^e)*m)+a を図で

[c]^[e]*-[m] <BR>
+-[a]

と表現する(ただしnullポインタは計算に影響しないので表記を省く)ことにし、結合はe>m>aの順に強いとする。

1-1. A + B <BR>
[A] <BR>
+-[B]

1-2. A * B <BR>
[A]*-[B]

1-3. A ^ B <BR>
[A]^[B]

各パラメータは鎖のように連続でつなげることができる
2-1. A + B + C <BR>
[A] <BR>
+-[B] <BR>
  +-[C] <BR>

2-2. A * B * C <BR>
[A]*-[B]*-[C]

2-3. A^(B^C) <BR>
[A]^[B]^[C]

※一般に(A^B)^CとA^(B^C)は異なるため注意が必要だ

マイナスは-1に値がかけられているものとして扱う
3. A - B <BR>
[A] <BR>
+-[-1]*-[B]

和で表される因数は結合の強さに逆らうので囲い込むコンテナが必要だ
4-1. (A + B) * C <BR>
[[A]]*C <BR>
 +-[B]

4-2. AC + B <BR>
[A]*C <BR>
+-[B] 

4-3. (A + B) * (C + D) <BR>
[[A]]*-[C] <BR>
 +[B]  +[D]

4-4. (A + B) * C + D <BR>
[[A]]*-[C] <BR>
|+[B] <BR>
+[D]

4-5. (A + B)^2 <BR>
[[A]]^2 <BR>
 +[B]

さけるべき無駄なコンテナ
5-1. (A) = A <BR>
[[A]]

5-2. (A + B) + C = A+B+C <BR>
[[A]] <BR>
|+[B] <BR>
+[C] <BR>

5-3. A * ((B + C)) = A*(B+C)
[A]*-[[B]]
      +[C]
※後ろの因数には囲い込むコンテナは必要はない

## TODO
・とりあえず因数のくくりだしなど計算できるように

・可換でない環・体への対応
