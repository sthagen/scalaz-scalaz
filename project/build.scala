import sbt._
import Keys._

import GenTypeClass._

import java.awt.Desktop

import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._

import com.jsuereth.sbtpgp.SbtPgp.autoImport.PgpKeys.{publishSigned, publishLocalSigned}

import com.typesafe.sbt.osgi.OsgiKeys
import com.typesafe.sbt.osgi.SbtOsgi

import sbtbuildinfo.BuildInfoPlugin.autoImport._

import com.typesafe.tools.mima.core.{ProblemFilters, IncompatibleSignatureProblem, InheritedNewAbstractMethodProblem}
import com.typesafe.tools.mima.plugin.MimaPlugin
import com.typesafe.tools.mima.plugin.MimaKeys.{mimaPreviousArtifacts, mimaReportSignatureProblems, mimaBinaryIssueFilters}

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import scalanativecrossproject.ScalaNativeCrossPlugin.autoImport._
import scalanative.sbtplugin.ScalaNativePlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._

import xerial.sbt.Sonatype.autoImport._

object build {
  type Sett = Def.Setting[_]

  val rootNativeId = "rootNative"

  lazy val publishSignedArtifacts = ReleaseStep(
    action = st => {
      val extracted = st.extract
      val ref = extracted.get(thisProjectRef)
      extracted.runAggregated(ref / (Global / publishSigned), st)
    },
    check = st => {
      // getPublishTo fails if no publish repository is set up.
      val ex = st.extract
      val ref = ex.get(thisProjectRef)
      val (newState, value) = ex.runTask(ref / (Global / publishTo), st)
      Classpaths.getPublishTo(value)
      newState
    },
    enableCrossBuild = true
  )

  lazy val setMimaVersion: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)
    val (releaseV, _) = st.get(ReleaseKeys.versions).getOrElse(sys.error("impossible"))
    IO.write(extracted get releaseVersionFile, s"""\nThisBuild / build.scalazMimaBasis := "${releaseV}"\n""", append = true)
    reapply(Seq(ThisBuild / scalazMimaBasis := releaseV), st)
  }

  val kindProjectorVersion = SettingKey[String]("kindProjectorVersion")

  private[this] def gitHash(): String = sys.process.Process("git rev-parse HEAD").lineStream_!.head

  private[this] val tagName = Def.setting{
    s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
  }
  private[this] val tagOrHash = Def.setting{
    if(isSnapshot.value) gitHash() else tagName.value
  }

  val scalajsProjectSettings = Seq[Sett](
    scalacOptions += {
      val a = (LocalRootProject / baseDirectory).value.toURI.toString
      val g = "https://raw.githubusercontent.com/scalaz/scalaz/" + tagOrHash.value

      val key = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          "-scalajs-mapSourceURI"
        case _ =>
          "-P:scalajs:mapSourceURI"
      }
      s"${key}:$a->$g/"
    },
    mimaPreviousArtifacts := {
      scalazMimaBasis.?.value.map {
        organization.value % s"${name.value}_sjs1_${scalaBinaryVersion.value}" % _
      }.toSet
    }
  )

  lazy val notPublish = Seq(
    publishArtifact := false,
    publish := {},
    publishLocal := {},
    publishSigned := {},
    publishLocalSigned := {},
    mimaPreviousArtifacts := Set.empty,
  )

  // avoid move files
  object ScalazCrossType extends sbtcrossproject.CrossType {
    override def projectDir(crossBase: File, projectType: String) =
      crossBase / projectType

    override def projectDir(crossBase: File, projectType: sbtcrossproject.Platform) = {
      val dir = projectType match {
        case JVMPlatform => "jvm"
        case JSPlatform => "js"
        case NativePlatform => "native"
      }
      crossBase / dir
    }

    def shared(projectBase: File, conf: String) =
      projectBase.getParentFile / "src" / conf / "scala"

    override def sharedSrcDir(projectBase: File, conf: String) =
      Some(shared(projectBase, conf))
  }

  private val stdOptions = Seq(
    "-opt:l:method",
    "-deprecation",
    "-Xlint:adapted-args",
    "-encoding", "UTF-8",
    "-feature",
    "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-language:postfixOps",
    "-unchecked"
  )

  val unusedWarnOptions = Def.setting {
    Seq("-Ywarn-unused:imports")
  }

  val lintOptions = Seq(
    "-Xlint:_,-type-parameter-shadow,-missing-interpolator",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    // "-Yrangepos" https://github.com/scala/bug/issues/10706
  )

  private def Scala212 = "2.12.20"
  private def Scala213 = "2.13.15"
  private def Scala3 = "3.3.4"

  private[this] val buildInfoPackageName = "scalaz"

  lazy val standardSettings: Seq[Sett] = Def.settings(
    organization := "org.scalaz",
    Seq(Compile, Test).map { scope =>
      (scope / unmanagedSourceDirectories) += {
        val dir = Defaults.nameForSrc(scope.name)
        val base = ScalazCrossType.shared(baseDirectory.value, dir).getParentFile
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, v)) if v <= 12 =>
            base / "scala-2.13-"
          case _ =>
            base / "scala-2.13+"
        }
      }
    },
    Compile / doc / sources := {
      scalaBinaryVersion.value match {
        case "3" =>
          // TODO OutOfMemoryError
          Nil
        case _ =>
          (Compile / doc / sources).value
      }
    },
    (Compile / packageSrc / mappings) ++= (Compile / managedSources).value.map{ f =>
      // https://github.com/sbt/sbt-buildinfo/blob/v0.7.0/src/main/scala/sbtbuildinfo/BuildInfoPlugin.scala#L58
      val buildInfoDir = "sbt-buildinfo"
      val path = if(f.getAbsolutePath.contains(buildInfoDir)) {
        (file(buildInfoPackageName) / f.relativeTo((Compile / sourceManaged).value / buildInfoDir).get.getPath).getPath
      } else {
        f.relativeTo((Compile / sourceManaged).value).get.getPath
      }
      (f, path)
    },
    scalaVersion := Scala212,
    crossScalaVersions := Seq(Scala212, Scala213, Scala3),
    addCommandAlias("SetScala2_12", s"++ ${Scala212}! -v"),
    addCommandAlias("SetScala2_13", s"++ ${Scala213}! -v"),
    addCommandAlias("SetScala3", s"++ ${Scala3}! -v"),
    fullResolvers ~= {_.filterNot(_.name == "jcenter")}, // https://github.com/sbt/sbt/issues/2217
    scalacOptions ++= stdOptions,
    scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((2, v)) if v <= 12 =>
        Seq(
          "-Xfuture",
          "-Ypartial-unification",
        )
    }.toList.flatten,
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 12 =>
          Seq(
            "-opt:l:method,inline",
            "-opt-inline-from:scalaz.**"
          )
        case _ =>
          Nil
      }
    },
    scalacOptions ++= lintOptions,
    scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((0 | 3, _)) =>
        Seq(
          "-source", "3.0-migration",
          "-Ykind-projector",
        )
    }.toList.flatten,
    scalacOptions ++= unusedWarnOptions.value,
    Seq(Compile, Test).flatMap(c =>
      (c / console / scalacOptions) --= unusedWarnOptions.value
    ),

    scala213_pre_cross_setting,

    (Compile / doc / scalacOptions) ++= {
      val base = (LocalRootProject / baseDirectory).value.getAbsolutePath
      Seq("-sourcepath", base, "-doc-source-url", "https://github.com/scalaz/scalaz/tree/" + tagOrHash.value + "€{FILE_PATH}.scala")
    },

    // retronym: I was seeing intermittent heap exhaustion in scalacheck based tests, so opting for determinism.
    Test / parallelExecution := false,
    genTypeClasses := {
      val s = streams.value
      typeClasses.value.flatMap { tc =>
        val dir = ScalazCrossType.shared(baseDirectory.value, "main")
        typeclassSource(tc).sources.map(_.createOrUpdate(dir, s.log))
      }
    },
    checkGenTypeClasses := {
      val classes = genTypeClasses.value
      if(classes.exists(_._1 != FileStatus.NoChange))
        sys.error(classes.groupBy(_._1).filterKeys(_ != FileStatus.NoChange).mapValues(_.map(_._2)).toString)
    },
    typeClasses := Seq(),
    genToSyntax := {
      val tcs = typeClasses.value
      val objects = tcs.map(tc => "object %s extends To%sSyntax".format(ScalazUtil.initLower(tc.name), tc.name)).mkString("\n")
      val all = "object all extends " + tcs.map(tc => "To%sSyntax".format(tc.name)).mkString(" with ")
      objects + "\n\n" + all
    },
    typeClassTree := {
      typeClasses.value.map(_.doc).mkString("\n")
    },

    (Compile / showDoc) := {
      val _ = (Compile / doc).value
      val out = (Compile / doc / target).value
      val index = out / "index.html"
      if (index.exists()) Desktop.getDesktop.open(out / "index.html")
    },

    credentialsSetting,
    publishTo := sonatypePublishToBundle.value,
    sonatypeBundleDirectory := {
      (LocalRootProject / target).value / "sonatype-staging" / (ThisBuild / version).value
    },
    Test / publishArtifact := false,

    // adapted from sbt-release defaults
    // (performs `publish-signed` instead of `publish`)
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("set ThisBuild / useSuperShell := false"),
      publishSignedArtifacts,
      releaseStepCommandAndRemaining(s"+ ${rootNativeId}/publishSigned"),
      releaseStepCommandAndRemaining("set ThisBuild / useSuperShell := true"),
      releaseStepCommandAndRemaining("sonatypeBundleRelease"),
      setNextVersion,
      setMimaVersion,
      commitNextVersion,
      pushChanges
    ),
    releaseTagName := tagName.value,
    pomIncludeRepository := {
      x => false
    },
    scmInfo := Some(ScmInfo(
      browseUrl = url("https://github.com/scalaz/scalaz"),
      connection = "scm:git:git@github.com:scalaz/scalaz.git"
    )),
    pomExtra := (
      <url>http://scalaz.org</url>
        <licenses>
          <license>
            <name>BSD-style</name>
            <url>https://opensource.org/licenses/BSD-3-Clause</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <developers>
          {
          Seq(
            ("runarorama", "Runar Bjarnason"),
            ("pchiusano", "Paul Chiusano"),
            ("tonymorris", "Tony Morris"),
            ("retronym", "Jason Zaugg"),
            ("ekmett", "Edward Kmett"),
            ("alexeyr", "Alexey Romanov"),
            ("copumpkin", "Daniel Peebles"),
            ("rwallace", "Richard Wallace"),
            ("nuttycom", "Kris Nuttycombe"),
            ("larsrh", "Lars Hupel")
          ).map {
            case (id, name) =>
              <developer>
                <id>{id}</id>
                <name>{name}</name>
                <url>https://github.com/{id}</url>
              </developer>
          }
        }
        </developers>
      ),

    licenseFile := {
      val LICENSE_txt = (ThisBuild / baseDirectory).value / "LICENSE.txt"
      if (!LICENSE_txt.exists()) sys.error(s"cannot find license file at $LICENSE_txt")
      LICENSE_txt
    },
    // kind-projector plugin
    kindProjectorVersion := "0.13.3",
    libraryDependencies ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((2, _)) =>
        Seq(
          compilerPlugin("org.typelevel" % "kind-projector" % kindProjectorVersion.value cross CrossVersion.full)
        )
    }.toList.flatten
  ) ++ Seq(packageBin, packageDoc, packageSrc).flatMap {
    // include LICENSE.txt in all packaged artifacts
    inTask(_)(Seq((Compile / mappings) += licenseFile.value -> "LICENSE"))
  } ++ SbtOsgi.projectSettings ++ Seq[Sett](
    OsgiKeys.additionalHeaders := Map("-removeheaders" -> "Include-Resource,Private-Package")
  ) ++ Def.settings(
    ThisBuild / mimaReportSignatureProblems := (scalaBinaryVersion.value != "3"),
    mimaPreviousArtifacts := {
      scalazMimaBasis.?.value.map {
        organization.value % s"${name.value}_${scalaBinaryVersion.value}" % _
      }.toSet
    }
  )

  private[this] val jvm_js_settings = Seq(
    (Compile / unmanagedSourceDirectories) += {
      baseDirectory.value.getParentFile / "jvm_js/src/main/scala/"
    }
  )

  private[this] val scala213_pre_cross_setting = {
    // sbt wants `scala-2.13.0-M1`, `scala-2.13.0-M2`, ... (sbt/sbt#2819)
    // @fommil tells me we could use sbt-sensible for this
    (Compile / unmanagedSourceDirectories) ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2L, minor)) =>
          Some((Compile / baseDirectory).value.getParentFile / s"src/main/scala-2.$minor")
        case _               =>
          None
      }
    }
  }

  val nativeSettings = Seq(
    Compile / doc / scalacOptions --= {
      // TODO remove this workaround
      // https://github.com/scala-native/scala-native/issues/2503
      if (scalaBinaryVersion.value == "3") {
        (Compile / doc / scalacOptions).value.filter(_.contains("-Xplugin"))
      } else {
        Nil
      }
    },
    mimaPreviousArtifacts := {
      scalazMimaBasis.?.value.map {
        organization.value % s"${name.value}_native0.4_${scalaBinaryVersion.value}" % _
      }.toSet
    },
  )

  lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(ScalazCrossType)
    .settings(standardSettings: _*)
    .settings(
      name := "scalaz-core",
      (Compile / sourceGenerators) += (Compile / sourceManaged).map{
        dir => Seq(GenerateTupleW(dir), TupleNInstances(dir))
      }.taskValue,
      buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
      buildInfoPackage := buildInfoPackageName,
      buildInfoObject := "ScalazBuildInfo",
      osgiExport("scalaz"),
      OsgiKeys.importPackage := Seq("javax.swing;resolution:=optional", "*"))
    .enablePlugins(sbtbuildinfo.BuildInfoPlugin, MimaPlugin)
    .jsSettings(
      jvm_js_settings,
      scalajsProjectSettings,
    )
    .jvmSettings(
      jvm_js_settings,
      typeClasses := TypeClass.core
    )
    .nativeSettings(
      nativeSettings
    )

  lazy val effect = crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(ScalazCrossType)
    .settings(standardSettings: _*)
    .settings(
      name := "scalaz-effect",
      osgiExport("scalaz.effect", "scalaz.std.effect", "scalaz.syntax.effect"))
    .dependsOn(core)
    .enablePlugins(MimaPlugin)
    .jsSettings(scalajsProjectSettings : _*)
    .jvmSettings(
      typeClasses := TypeClass.effect
    )
    .nativeSettings(
      nativeSettings
    )

  lazy val iteratee = crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(ScalazCrossType)
    .settings(standardSettings: _*)
    .settings(
      name := "scalaz-iteratee",
      osgiExport("scalaz.iteratee"))
    .dependsOn(core, effect)
    .enablePlugins(MimaPlugin)
    .jsSettings(scalajsProjectSettings : _*)
    .nativeSettings(
      nativeSettings
    )

  lazy val credentialsSetting = credentials ++= {
    val name = "Sonatype Nexus Repository Manager"
    val realm = "oss.sonatype.org"
    (
      sys.props.get("build.publish.user"),
      sys.props.get("build.publish.password"),
      sys.env.get("SONATYPE_USERNAME"),
      sys.env.get("SONATYPE_PASSWORD")
    ) match {
      case (Some(user), Some(pass), _, _)  => Seq(Credentials(name, realm, user, pass))
      case (_, _, Some(user), Some(pass))  => Seq(Credentials(name, realm, user, pass))
      case _                           =>
        val ivyFile = Path.userHome / ".ivy2" / ".credentials"
        val m2File = Path.userHome / ".m2" / "credentials"
        if (ivyFile.exists()) Seq(Credentials(ivyFile))
        else if (m2File.exists()) Seq(Credentials(m2File))
        else Nil
    }
  }

  lazy val licenseFile = settingKey[File]("The license file to include in packaged artifacts")

  lazy val scalazMimaBasis = settingKey[String]("Version of scalaz against which to run MIMA.")

  lazy val genTypeClasses = taskKey[Seq[(FileStatus, File)]]("")

  lazy val typeClasses = taskKey[Seq[TypeClass]]("")

  lazy val genToSyntax = taskKey[String]("")

  lazy val showDoc = taskKey[Unit]("")

  lazy val typeClassTree = taskKey[String]("Generates scaladoc formatted tree of type classes.")

  lazy val checkGenTypeClasses = taskKey[Unit]("")

  def osgiExport(packs: String*) = OsgiKeys.exportPackage := packs.map(_ + ".*;version=${Bundle-Version}")
}

// vim: expandtab:ts=2:sw=2
