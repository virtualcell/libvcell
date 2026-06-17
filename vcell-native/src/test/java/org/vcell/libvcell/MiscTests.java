package org.vcell.libvcell;

import cbit.vcell.mapping.MappingException;
import cbit.vcell.xml.XmlParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.vcell.libvcell.ModelUtils.vcml_to_vcml;

public class MiscTests {

	@Test
	public void testErrorMessageCreation() throws IOException {
		String vcmlContent;
		try (InputStream is = MiscTests.class.getResourceAsStream("/bad_vcml.vcml")) {
			Assertions.assertNotNull(is);
			vcmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		Path ignored = Files.createTempFile("dummy", ".vcml");
		try {
			vcml_to_vcml(vcmlContent, ignored);
		} catch (Exception e) {
			String actualErrMsg = Entrypoints.generateErrorReport("Something went wrong!", e).strip();
			Assertions.assertEquals(MiscTests.testErrorMessageCreation_expectedError, actualErrMsg);
			return;
		}
		throw new IllegalStateException("An exception was not thrown when one should have been!");
	}

	private static final String testErrorMessageCreation_expectedError = """
Something went wrong!
Error:
0 --> XmlParseException :: Error setting Velocity parameter for 'H3
1 -----> ExpressionBindingException :: 'norm_X' is either not found in your model or is not allowed to be used in the current context. Check that you have provided the correct and full name (e.g. Ca_Cytosol).

Stack Traces:
1) ExpressionBindingException:
cbit.vcell.parser.ASTIdNode.bind(ASTIdNode.java:67)
cbit.vcell.parser.SimpleNode.bind(SimpleNode.java:54)
cbit.vcell.parser.SimpleNode.bind(SimpleNode.java:54)
cbit.vcell.parser.SimpleNode.bind(SimpleNode.java:54)
cbit.vcell.parser.SimpleNode.bind(SimpleNode.java:54)
cbit.vcell.parser.Expression.bindExpression(Expression.java:164)
cbit.vcell.mapping.SpeciesContextSpec$SpeciesContextSpecParameter.setExpression(SpeciesContextSpec.java:270)
cbit.vcell.xml.XmlReader.getSpeciesContextSpecs(XmlReader.java:7115)

0) XmlParseException:
cbit.vcell.xml.XmlReader.getSpeciesContextSpecs(XmlReader.java:7135)
cbit.vcell.xml.XmlReader.getSimulationContext(XmlReader.java:6227)
cbit.vcell.xml.XmlReader.getBioModel(XmlReader.java:421)
cbit.vcell.xml.XmlHelper.XMLToBioModel(XmlHelper.java:630)
cbit.vcell.xml.XmlHelper.XMLToBioModel(XmlHelper.java:504)
org.vcell.libvcell.ModelUtils.vcml_to_vcml(ModelUtils.java:123)
org.vcell.libvcell.MiscTests.testErrorMessageCreation(MiscTests.java:27)
java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
java.base/java.lang.reflect.Method.invoke(Method.java:565)
org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218)
org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214)
org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139)
org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198)
org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169)
org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93)
org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58)
org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141)
org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57)
org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
org.junit.platform.launcher.core.SessionPerRequestLauncher.execute(SessionPerRequestLauncher.java:63)
com.intellij.junit5.JUnit5TestRunnerHelper.execute(JUnit5TestRunnerHelper.java:134)
com.intellij.junit5.JUnit5IdeaTestRunner.startRunnerWithArgs(JUnit5IdeaTestRunner.java:70)
com.intellij.rt.junit.IdeaTestRunner$Repeater$1.execute(IdeaTestRunner.java:38)
com.intellij.rt.execution.junit.TestsRepeater.repeat(TestsRepeater.java:11)
com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:35)
com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:225)
com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:61)
		""".strip();


}
