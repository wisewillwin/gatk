package oneoffs.depristo

import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.samtools.SamtoolsIndexFunction
import org.broadinstitute.sting.queue.extensions.gatk._
import org.broadinstitute.sting.queue.function.JavaCommandLineFunction

class ExomePostQCEval extends QScript {
  @Argument(doc="gatkJarFile", required=false)
  var gatkJarFile: File = new File("/home/radon01/depristo/dev/GenomeAnalysisTK/trunk/dist/GenomeAnalysisTK.jar")

  @Argument(shortName = "R", doc="ref", required=false)
  var referenceFile: File = new File("/humgen/1kg/reference/human_g1k_v37.fasta")

  @Argument(shortName = "eval", doc="ref", required=true)
  var evalVCF: File = _

  @Argument(shortName = "intervals", doc="intervals", required=true)
  val myIntervals: String = null;

  @Argument(shortName = "dbSNP", doc="dbSNP", required=false)
  val dbSNP: File = new File("/humgen/gsa-hpprojects/GATK/bundle/current/b37/dbsnp_132.b37.vcf")

  trait UNIVERSAL_GATK_ARGS extends CommandLineGATK {
    this.logging_level = "INFO";
    this.jarFile = gatkJarFile;
    this.reference_sequence = referenceFile;
    this.memoryLimit = 4
  }

  // TODO -- should include "standard" eval for plotting expectations

  def script = {
    // The basic summary eval
    createEval(".summary",
               List("TiTvVariantEvaluator", "CountVariants", "CompOverlap"),
               List("EvalRod", "CompRod", "Novelty", "FunctionalClass"))

    // The basic summary eval, by AF
    createEval(".byAF",
               List("TiTvVariantEvaluator", "CountVariants", "CompOverlap"),
               List("EvalRod", "CompRod", "Novelty", "AlleleFrequency", "FunctionalClass"))

    // By sample
    createEval(".bySample",
               List("TiTvVariantEvaluator", "CountVariants", "CompOverlap"),
               List("EvalRod", "CompRod", "Novelty", "Sample"))
  }

  def createEval(prefix: String, evalModules: List[String], extraStrats: List[String]) {
    val eval = new Eval(evalVCF)
    eval.out = swapExt(evalVCF,".vcf", prefix + ".eval")
    eval.evalModule = evalModules
    eval.stratificationModule = List("EvalRod", "CompRod", "Novelty") ::: extraStrats
    add(eval)
  }

  class Eval(@Input vcf: File) extends VariantEval with UNIVERSAL_GATK_ARGS {
    this.rodBind :+= RodBind("eval", "VCF", evalVCF)
    if ( dbSNP.exists() )
      this.rodBind :+= RodBind("dbsnp", "VCF", dbSNP)
    this.doNotUseAllStandardStratifications = true
    this.doNotUseAllStandardModules = true
    this.intervalsString = List(myIntervals);
  }
}
