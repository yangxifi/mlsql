package tech.mlsql.test

import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import tech.mlsql.ets.SQLGenContext
import tech.mlsql.lang.cmd.compile.internal.gc._

/**
 * 6/10/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class ExprTest extends FunSuite with BeforeAndAfterAll {
  var spark: SparkSession = null

  override def beforeAll(): Unit = {
    spark = SparkSession.builder().
      master("local[*]").
      appName("test").
      getOrCreate()
  }

  override def afterAll(): Unit = {
    if (spark != null) {
      spark.close()
    }
  }

  def evaluate(str: String, input: Map[String, String]): Any = {
    val scanner = new Scanner(str)
    val tokenizer = new Tokenizer(scanner)
    val parser = new StatementParser(tokenizer)
    val exprs = parser.parse()
    val sQLGenContext = new SQLGenContext(spark)
    val item = sQLGenContext.execute(exprs.map(_.asInstanceOf[Expression]), input)
    return item
  }

  test("codegen1") {

    val input = Map("a" -> "jack,20")
    val item = evaluate(
      """
        |select split(:a,",")[0] as :jack,"" as :bj;
        |(:jack=="jack" and 1==1) and :bj>=24
        |""".stripMargin, input)

    assert(item == Literal(false, Types.Boolean))
  }

  test("codegen2") {

    val input = Map("a" -> "jack,20")

    var item = evaluate(
      """
        |select split(:a,",")[0] as :jack,cast(split(:a,",")[1] as float) as :bj;
        |(:jack=="jack" and 1==1) and cast(:bj as int)>=7
        |""".stripMargin, input)

    assert(item == Literal(true, Types.Boolean))

    item = evaluate(
      """
        |select split(:a,",")[0] as :jack,cast(split(:a,",")[1] as float) as :bj;
        |(:jack=="jack" and 1==1) and cast(:bj as int)>=33
        |""".stripMargin, input)

    assert(item == Literal(false, Types.Boolean))

    item = evaluate(
      """
        |select split(:a,",")[0] as :jack,cast(split(:a,",")[1] as float) as :bj;
        |(:jack=="jack1" and 1==1) and cast(:bj as int)>=7
        |""".stripMargin, input)

    assert(item == Literal(false, Types.Boolean))
  }
}