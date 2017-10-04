/*
 * Copyright (C) 2014 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.internal.codegen;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static dagger.internal.codegen.Compilers.daggerCompiler;
import static dagger.internal.codegen.GeneratedLines.GENERATED_ANNOTATION;
import static dagger.internal.codegen.GeneratedLines.NPE_FROM_PROVIDES_METHOD;

import com.google.auto.value.processor.AutoAnnotationProcessor;
import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.util.Collection;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MapBindingComponentProcessorTest {
  @Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return CompilerMode.TEST_PARAMETERS;
  }

  private final CompilerMode compilerMode;

  public MapBindingComponentProcessorTest(CompilerMode compilerMode) {
    this.compilerMode = compilerMode;
  }

  @Test
  public void mapBindingsWithEnumKey() {
    JavaFileObject mapModuleOneFile =
        JavaFileObjects
            .forSourceLines("test.MapModuleOne",
                "package test;",
                "",
                "import dagger.Module;",
                "import dagger.Provides;",
                "import dagger.multibindings.IntoMap;",
                "",
                "@Module",
                "final class MapModuleOne {",
                "  @Provides @IntoMap @PathKey(PathEnum.ADMIN) Handler provideAdminHandler() {",
                "    return new AdminHandler();",
                "  }",
                "}");
    JavaFileObject mapModuleTwoFile =
        JavaFileObjects
            .forSourceLines("test.MapModuleTwo",
                "package test;",
                "",
                "import dagger.Module;",
                "import dagger.Provides;",
                "import dagger.multibindings.IntoMap;",
                "",
                "@Module",
                "final class MapModuleTwo {",
                "  @Provides @IntoMap @PathKey(PathEnum.LOGIN) Handler provideLoginHandler() {",
                "    return new LoginHandler();",
                "  }",
                "}");
    JavaFileObject enumKeyFile = JavaFileObjects.forSourceLines("test.PathKey",
        "package test;",
        "import dagger.MapKey;",
        "import java.lang.annotation.Retention;",
        "import static java.lang.annotation.RetentionPolicy.RUNTIME;",
        "",
        "@MapKey(unwrapValue = true)",
        "@Retention(RUNTIME)",
        "public @interface PathKey {",
        "  PathEnum value();",
        "}");
    JavaFileObject pathEnumFile = JavaFileObjects.forSourceLines("test.PathEnum",
        "package test;",
        "",
        "public enum PathEnum {",
        "    ADMIN,",
        "    LOGIN;",
        "}");

    JavaFileObject HandlerFile = JavaFileObjects.forSourceLines("test.Handler",
        "package test;",
        "",
        "interface Handler {}");
    JavaFileObject LoginHandlerFile = JavaFileObjects.forSourceLines("test.LoginHandler",
        "package test;",
        "",
        "class LoginHandler implements Handler {",
        "  public LoginHandler() {}",
        "}");
    JavaFileObject AdminHandlerFile = JavaFileObjects.forSourceLines("test.AdminHandler",
        "package test;",
        "",
        "class AdminHandler implements Handler {",
        "  public AdminHandler() {}",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.TestComponent",
        "package test;",
        "",
        "import dagger.Component;",
        "import java.util.Map;",
        "import javax.inject.Provider;",
        "",
        "@Component(modules = {MapModuleOne.class, MapModuleTwo.class})",
        "interface TestComponent {",
        "  Provider<Map<PathEnum, Provider<Handler>>> dispatcher();",
        "}");
    JavaFileObject generatedComponent =
        JavaFileObjects.forSourceLines(
            "test.DaggerTestComponent",
            "package test;",
            "",
            "import dagger.internal.MapProviderFactory;",
            "import dagger.internal.Preconditions;",
            "import java.util.Map;",
            "import javax.annotation.Generated;",
            "import javax.inject.Provider;",
            "",
            GENERATED_ANNOTATION,
            "public final class DaggerTestComponent implements TestComponent {",
            "  private Provider<Handler> provideAdminHandlerProvider;",
            "  private Provider<Handler> provideLoginHandlerProvider;",
            "  private Provider<Map<PathEnum, Provider<Handler>>>",
            "      mapOfPathEnumAndProviderOfHandlerProvider;",
            "",
            "  private DaggerTestComponent(Builder builder) {",
            "    initialize(builder);",
            "  }",
            "",
            "  public static Builder builder() {",
            "    return new Builder();",
            "  }",
            "",
            "  public static TestComponent create() {",
            "    return new Builder().build();",
            "  }",
            "",
            "  @SuppressWarnings(\"unchecked\")",
            "  private void initialize(final Builder builder) {",
            "    this.provideAdminHandlerProvider =",
            "        MapModuleOne_ProvideAdminHandlerFactory.create(builder.mapModuleOne);",
            "    this.provideLoginHandlerProvider =",
            "        MapModuleTwo_ProvideLoginHandlerFactory.create(builder.mapModuleTwo);",
            "    this.mapOfPathEnumAndProviderOfHandlerProvider =",
            "        MapProviderFactory.<PathEnum, Handler>builder(2)",
            "            .put(PathEnum.ADMIN, provideAdminHandlerProvider)",
            "            .put(PathEnum.LOGIN, provideLoginHandlerProvider)",
            "            .build();",
            "  }",
            "",
            "  @Override",
            "  public Provider<Map<PathEnum, Provider<Handler>>> dispatcher() {",
            "    return mapOfPathEnumAndProviderOfHandlerProvider;",
            "  }",
            "",
            "  public static final class Builder {",
            "    private MapModuleOne mapModuleOne;",
            "    private MapModuleTwo mapModuleTwo;",
            "",
            "    private Builder() {",
            "    }",
            "",
            "    public TestComponent build() {",
            "      if (mapModuleOne == null) {",
            "        this.mapModuleOne = new MapModuleOne();",
            "      }",
            "      if (mapModuleTwo == null) {",
            "        this.mapModuleTwo = new MapModuleTwo();",
            "      }",
            "      return new DaggerTestComponent(this);",
            "    }",
            "",
            "    public Builder mapModuleOne(MapModuleOne mapModuleOne) {",
            "      this.mapModuleOne = Preconditions.checkNotNull(mapModuleOne);",
            "      return this;",
            "    }",
            "",
            "    public Builder mapModuleTwo(MapModuleTwo mapModuleTwo) {",
            "      this.mapModuleTwo = Preconditions.checkNotNull(mapModuleTwo);",
            "      return this;",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources())
        .that(
            ImmutableList.of(
                mapModuleOneFile,
                mapModuleTwoFile,
                enumKeyFile,
                pathEnumFile,
                HandlerFile,
                LoginHandlerFile,
                AdminHandlerFile,
                componentFile))
        .withCompilerOptions(compilerMode.javacopts())
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(generatedComponent);
  }

  @Test
  public void mapBindingsWithInaccessibleKeys() {
    JavaFileObject mapKeys =
        JavaFileObjects.forSourceLines(
            "mapkeys.MapKeys",
            "package mapkeys;",
            "",
            "import dagger.MapKey;",
            "import dagger.multibindings.ClassKey;",
            "",
            "public class MapKeys {",
            "  @MapKey(unwrapValue = false)",
            "  public @interface ComplexKey {",
            "    Class<?>[] manyClasses();",
            "    Class<?> oneClass();",
            "    ClassKey annotation();",
            "  }",
            "",
            "  @MapKey",
            "  @interface EnumKey {",
            "    PackagePrivateEnum value();",
            "  }",
            "",
            "  enum PackagePrivateEnum { INACCESSIBLE }",
            "",
            "  interface Inaccessible {}",
            "}");
    JavaFileObject moduleFile =
        JavaFileObjects.forSourceLines(
            "mapkeys.MapModule",
            "package mapkeys;",
            "",
            "import dagger.Binds;",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import dagger.multibindings.ClassKey;",
            "import dagger.multibindings.IntoMap;",
            "import java.util.Map;",
            "import javax.inject.Provider;",
            "",
            "@Module",
            "public interface MapModule {",
            "  @Provides @IntoMap @ClassKey(MapKeys.Inaccessible.class)",
            "  static int classKey() { return 1; }",
            "",
            "  @Provides @IntoMap @MapKeys.EnumKey(MapKeys.PackagePrivateEnum.INACCESSIBLE)",
            "  static int enumKey() { return 1; }",
            "",
            "  @Binds Object bindInaccessibleEnumMapToAccessibleTypeForComponent(",
            "    Map<MapKeys.PackagePrivateEnum, Integer> map);",
            "",
            "  @Provides @IntoMap",
            "  @MapKeys.ComplexKey(",
            "    manyClasses = {java.lang.Object.class, java.lang.String.class},",
            "    oneClass = MapKeys.Inaccessible.class,",
            "    annotation = @ClassKey(java.lang.Object.class)",
            "  )",
            "  static int complexKeyWithInaccessibleValue() { return 1; }",
            "",
            "  @Provides @IntoMap",
            "  @MapKeys.ComplexKey(",
            "    manyClasses = {MapKeys.Inaccessible.class, java.lang.String.class},",
            "    oneClass = java.lang.String.class,",
            "    annotation = @ClassKey(java.lang.Object.class)",
            "  )",
            "  static int complexKeyWithInaccessibleArrayValue() { return 1; }",
            "",
            "  @Provides @IntoMap",
            "  @MapKeys.ComplexKey(",
            "    manyClasses = {java.lang.String.class},",
            "    oneClass = java.lang.String.class,",
            "    annotation = @ClassKey(MapKeys.Inaccessible.class)",
            "  )",
            "  static int complexKeyWithInaccessibleAnnotationValue() { return 1; }",
            "}");
    JavaFileObject componentFile =
        JavaFileObjects.forSourceLines(
            "test.TestComponent",
            "package test;",
            "",
            "import dagger.Component;",
            "import java.util.Map;",
            "import javax.inject.Provider;",
            "import mapkeys.MapKeys;",
            "import mapkeys.MapModule;",
            "",
            "@Component(modules = MapModule.class)",
            "interface TestComponent {",
            "  Map<Class<?>, Integer> classKey();",
            "  Provider<Map<Class<?>, Integer>> classKeyProvider();",
            "",
            "  Object inaccessibleEnum();",
            "  Provider<Object> inaccessibleEnumProvider();",
            "",
            "  Map<MapKeys.ComplexKey, Integer> complexKey();",
            "  Provider<Map<MapKeys.ComplexKey, Integer>> complexKeyProvider();",
            "}");
    Compilation compilation = daggerCompiler().compile(mapKeys, moduleFile, componentFile);
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .generatedSourceFile("test.DaggerTestComponent")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceLines(
                "test.DaggerTestComponent",
                "package test;",
                "",
                "import com.google.common.collect.ImmutableMap;",
                "import dagger.internal.MapFactory;",
                "import java.util.Map;",
                "import javax.annotation.Generated;",
                "import javax.inject.Provider;",
                "import mapkeys.MapKeys;",
                "import mapkeys.MapModule;",
                "import mapkeys.MapModule_ClassKeyFactory;",
                "import mapkeys.MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory;",
                "import mapkeys.MapModule_ComplexKeyWithInaccessibleArrayValueFactory;",
                "import mapkeys.MapModule_ComplexKeyWithInaccessibleValueFactory;",
                "import mapkeys.MapModule_EnumKeyFactory;",
                "",
                GENERATED_ANNOTATION,
                "public final class DaggerTestComponent implements TestComponent {",
                "  private Provider<Map<Class<?>, Integer>> mapOfClassOfAndIntegerProvider;",
                "",
                "  @SuppressWarnings(\"rawtypes\")",
                "  private Provider mapOfPackagePrivateEnumAndIntegerProvider;",
                "",
                "  private Provider<Map<MapKeys.ComplexKey, Integer>>",
                "      mapOfComplexKeyAndIntegerProvider;",
                "",
                "  private DaggerTestComponent(Builder builder) {",
                "    initialize(builder);",
                "  }",
                "",
                "  public static Builder builder() {",
                "    return new Builder();",
                "  }",
                "",
                "  public static TestComponent create() {",
                "    return new Builder().build();",
                "  }",
                "",
                "  @SuppressWarnings(\"unchecked\")",
                "  private void initialize(final Builder builder) {",
                "    this.mapOfClassOfAndIntegerProvider =",
                "        MapFactory.<Class<?>, Integer>builder(1)",
                "            .put(MapModule_ClassKeyFactory.mapKey(),",
                "                 MapModule_ClassKeyFactory.create())",
                "            .build();",
                "    this.mapOfPackagePrivateEnumAndIntegerProvider =",
                "        MapFactory.builder(1)",
                "            .put(MapModule_EnumKeyFactory.mapKey(), ",
                "                 (Provider) MapModule_EnumKeyFactory.create())",
                "            .build();",
                "    this.mapOfComplexKeyAndIntegerProvider =",
                "       MapFactory.<MapKeys.ComplexKey, Integer>builder(3)",
                "          .put(",
                "             MapModule_ComplexKeyWithInaccessibleValueFactory.mapKey(),",
                "             MapModule_ComplexKeyWithInaccessibleValueFactory.create())",
                "          .put(",
                "             MapModule_ComplexKeyWithInaccessibleArrayValueFactory.mapKey(),",
                "             MapModule_ComplexKeyWithInaccessibleArrayValueFactory.create())",
                "          .put(",
                "             MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory.mapKey(),",
                "             MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory.create())",
                "          .build();",
                "  }",
                "",
                "  @Override",
                "  public Map<Class<?>, Integer> classKey() {",
                "    return ImmutableMap.<Class<?>, Integer>of(",
                "        MapModule_ClassKeyFactory.mapKey(), MapModule.classKey());",
                "  }",
                "",
                "  @Override",
                "  public Provider<Map<Class<?>, Integer>> classKeyProvider() {",
                "    return mapOfClassOfAndIntegerProvider;",
                "  }",
                "",
                "  @Override",
                "  public Object inaccessibleEnum() {",
                "    return ImmutableMap.of(",
                "        MapModule_EnumKeyFactory.mapKey(), MapModule.enumKey());",
                "  }",
                "",
                "  @Override",
                "  public Provider<Object> inaccessibleEnumProvider() {",
                "    return mapOfPackagePrivateEnumAndIntegerProvider;",
                "  }",
                "",
                "  @Override",
                "  public Map<MapKeys.ComplexKey, Integer> complexKey() {",
                "    return ImmutableMap.<MapKeys.ComplexKey, Integer>of(",
                "        MapModule_ComplexKeyWithInaccessibleValueFactory.mapKey(),",
                "        MapModule.complexKeyWithInaccessibleValue(),",
                "        MapModule_ComplexKeyWithInaccessibleArrayValueFactory.mapKey(),",
                "        MapModule.complexKeyWithInaccessibleArrayValue(),",
                "        MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory.mapKey(),",
                "        MapModule.complexKeyWithInaccessibleAnnotationValue());",
                "  }",
                "",
                "  @Override",
                "  public Provider<Map<MapKeys.ComplexKey, Integer>> complexKeyProvider() {",
                "    return mapOfComplexKeyAndIntegerProvider;",
                "  }",
                "",
                "  public static final class Builder {",
                "    private Builder() {}",
                "",
                "    public TestComponent build() {",
                "      return new DaggerTestComponent(this);",
                "    }",
                "  }",
                "}"));
    assertThat(compilation)
        .generatedSourceFile(
            "mapkeys.MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceLines(
                "mapkeys.MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory",
                "package mapkeys;",
                "",
                "import dagger.internal.Factory;",
                "import javax.annotation.Generated;",
                "",
                GENERATED_ANNOTATION,
                "public final class",
                "    MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory",
                "        implements Factory<Integer> {",
                "  private static final",
                "    MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory INSTANCE =",
                "      new MapModule_ComplexKeyWithInaccessibleAnnotationValueFactory();",
                "",
                "  @Override",
                "  public Integer get() {",
                "    return MapModule.complexKeyWithInaccessibleAnnotationValue();",
                "  }",
                "",
                "  public static Factory<Integer> create() {",
                "    return INSTANCE;",
                "  }",
                "",
                "  public static MapKeys.ComplexKey mapKey() {",
                "    return MapKeys_ComplexKeyCreator.createComplexKey(",
                "        new Class[] {String.class},",
                "        String.class,",
                "        MapKeys_ComplexKeyCreator.createClassKey(MapKeys.Inaccessible.class));",
                "  }",
                "}"));
    assertThat(compilation)
        .generatedSourceFile("mapkeys.MapModule_ClassKeyFactory")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceLines(
                "mapkeys.MapModule_ClassKeyFactory",
                "package mapkeys;",
                "",
                "import dagger.internal.Factory;",
                "import javax.annotation.Generated;",
                "",
                GENERATED_ANNOTATION,
                "public final class MapModule_ClassKeyFactory implements Factory<Integer> {",
                "  private static final MapModule_ClassKeyFactory INSTANCE = ",
                "      new MapModule_ClassKeyFactory();",
                "",
                "  @Override",
                "  public Integer get() {",
                "    return MapModule.classKey();",
                "  }",
                "",
                "  public static Factory<Integer> create() {",
                "    return INSTANCE;",
                "  }",
                "",
                "  public static Class<?> mapKey() {",
                "    return MapKeys.Inaccessible.class;",
                "  }",
                "}"));
  }

  @Test
  public void mapBindingsWithStringKey() {
    JavaFileObject mapModuleOneFile =
        JavaFileObjects
            .forSourceLines("test.MapModuleOne",
                "package test;",
                "",
                "import dagger.Module;",
                "import dagger.Provides;",
                "import dagger.multibindings.StringKey;",
                "import dagger.multibindings.IntoMap;",
                "",
                "@Module",
                "final class MapModuleOne {",
                "  @Provides @IntoMap @StringKey(\"Admin\") Handler provideAdminHandler() {",
                "    return new AdminHandler();",
                "  }",
                "}");
    JavaFileObject mapModuleTwoFile =
        JavaFileObjects
            .forSourceLines("test.MapModuleTwo",
                "package test;",
                "",
                "import dagger.Module;",
                "import dagger.Provides;",
                "import dagger.multibindings.IntoMap;",
                "import dagger.multibindings.StringKey;",
                "",
                "@Module",
                "final class MapModuleTwo {",
                "  @Provides @IntoMap @StringKey(\"Login\") Handler provideLoginHandler() {",
                "    return new LoginHandler();",
                "  }",
                "}");
    JavaFileObject HandlerFile = JavaFileObjects.forSourceLines("test.Handler",
        "package test;",
        "",
        "interface Handler {}");
    JavaFileObject LoginHandlerFile = JavaFileObjects.forSourceLines("test.LoginHandler",
        "package test;",
        "",
        "class LoginHandler implements Handler {",
        "  public LoginHandler() {}",
        "}");
    JavaFileObject AdminHandlerFile = JavaFileObjects.forSourceLines("test.AdminHandler",
        "package test;",
        "",
        "class AdminHandler implements Handler {",
        "  public AdminHandler() {}",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.TestComponent",
        "package test;",
        "",
        "import dagger.Component;",
        "import java.util.Map;",
        "import javax.inject.Provider;",
        "",
        "@Component(modules = {MapModuleOne.class, MapModuleTwo.class})",
        "interface TestComponent {",
        "  Provider<Map<String, Provider<Handler>>> dispatcher();",
        "}");
    JavaFileObject generatedComponent =
        JavaFileObjects.forSourceLines(
            "test.DaggerTestComponent",
            "package test;",
            "",
            "import dagger.internal.MapProviderFactory;",
            "import dagger.internal.Preconditions;",
            "import java.util.Map;",
            "import javax.annotation.Generated;",
            "import javax.inject.Provider;",
            "",
            GENERATED_ANNOTATION,
            "public final class DaggerTestComponent implements TestComponent {",
            "  private Provider<Handler> provideAdminHandlerProvider;",
            "  private Provider<Handler> provideLoginHandlerProvider;",
            "  private Provider<Map<String, Provider<Handler>>>",
            "      mapOfStringAndProviderOfHandlerProvider;",
            "",
            "  private DaggerTestComponent(Builder builder) {",
            "    initialize(builder);",
            "  }",
            "",
            "  public static Builder builder() {",
            "    return new Builder();",
            "  }",
            "",
            "  public static TestComponent create() {",
            "    return new Builder().build();",
            "  }",
            "",
            "  @SuppressWarnings(\"unchecked\")",
            "  private void initialize(final Builder builder) {",
            "    this.provideAdminHandlerProvider =",
            "        MapModuleOne_ProvideAdminHandlerFactory.create(builder.mapModuleOne);",
            "    this.provideLoginHandlerProvider =",
            "        MapModuleTwo_ProvideLoginHandlerFactory.create(builder.mapModuleTwo);",
            "    this.mapOfStringAndProviderOfHandlerProvider =",
            "        MapProviderFactory.<String, Handler>builder(2)",
            "            .put(\"Admin\", provideAdminHandlerProvider)",
            "            .put(\"Login\", provideLoginHandlerProvider)",
            "            .build();",
            "  }",
            "",
            "  @Override",
            "  public Provider<Map<String, Provider<Handler>>> dispatcher() {",
            "    return mapOfStringAndProviderOfHandlerProvider;",
            "  }",
            "",
            "  public static final class Builder {",
            "    private MapModuleOne mapModuleOne;",
            "    private MapModuleTwo mapModuleTwo;",
            "",
            "    private Builder() {",
            "    }",
            "",
            "    public TestComponent build() {",
            "      if (mapModuleOne == null) {",
            "        this.mapModuleOne = new MapModuleOne();",
            "      }",
            "      if (mapModuleTwo == null) {",
            "        this.mapModuleTwo = new MapModuleTwo();",
            "      }",
            "      return new DaggerTestComponent(this);",
            "    }",
            "",
            "    public Builder mapModuleOne(MapModuleOne mapModuleOne) {",
            "      this.mapModuleOne = Preconditions.checkNotNull(mapModuleOne);",
            "      return this;",
            "    }",
            "",
            "    public Builder mapModuleTwo(MapModuleTwo mapModuleTwo) {",
            "      this.mapModuleTwo = Preconditions.checkNotNull(mapModuleTwo);",
            "      return this;",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources())
        .that(
            ImmutableList.of(
                mapModuleOneFile,
                mapModuleTwoFile,
                HandlerFile,
                LoginHandlerFile,
                AdminHandlerFile,
                componentFile))
        .withCompilerOptions(compilerMode.javacopts())
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(generatedComponent);
  }

  @Test
  public void mapBindingsWithWrappedKey() {
    JavaFileObject mapModuleOneFile =
        JavaFileObjects
            .forSourceLines("test.MapModuleOne",
                "package test;",
                "",
                "import dagger.Module;",
                "import dagger.Provides;",
                "import dagger.multibindings.IntoMap;",
                "",
                "@Module",
                "final class MapModuleOne {",
                "  @Provides @IntoMap",
                "  @WrappedClassKey(Integer.class) Handler provideAdminHandler() {",
                "    return new AdminHandler();",
                "  }",
                "}");
    JavaFileObject mapModuleTwoFile =
        JavaFileObjects
            .forSourceLines("test.MapModuleTwo",
                "package test;",
                "",
                "import dagger.Module;",
                "import dagger.Provides;",
                "import dagger.multibindings.IntoMap;",
                "",
                "@Module",
                "final class MapModuleTwo {",
                "  @Provides @IntoMap",
                "  @WrappedClassKey(Long.class) Handler provideLoginHandler() {",
                "    return new LoginHandler();",
                "  }",
                "}");
    JavaFileObject wrappedClassKeyFile = JavaFileObjects.forSourceLines("test.WrappedClassKey",
        "package test;",
        "import dagger.MapKey;",
        "import java.lang.annotation.Retention;",
        "import static java.lang.annotation.RetentionPolicy.RUNTIME;",
        "",
        "@MapKey(unwrapValue = false)",
        "@Retention(RUNTIME)",
        "public @interface WrappedClassKey {",
        "  Class<?> value();",
        "}");
    JavaFileObject HandlerFile = JavaFileObjects.forSourceLines("test.Handler",
        "package test;",
        "",
        "interface Handler {}");
    JavaFileObject LoginHandlerFile = JavaFileObjects.forSourceLines("test.LoginHandler",
        "package test;",
        "",
        "class LoginHandler implements Handler {",
        "  public LoginHandler() {}",
        "}");
    JavaFileObject AdminHandlerFile = JavaFileObjects.forSourceLines("test.AdminHandler",
        "package test;",
        "",
        "class AdminHandler implements Handler {",
        "  public AdminHandler() {}",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.TestComponent",
        "package test;",
        "",
        "import dagger.Component;",
        "import java.util.Map;",
        "import javax.inject.Provider;",
        "",
        "@Component(modules = {MapModuleOne.class, MapModuleTwo.class})",
        "interface TestComponent {",
        "  Provider<Map<WrappedClassKey, Provider<Handler>>> dispatcher();",
        "}");
    JavaFileObject generatedComponent =
        JavaFileObjects.forSourceLines(
            "test.DaggerTestComponent",
            "package test;",
            "",
            "import dagger.internal.MapProviderFactory;",
            "import dagger.internal.Preconditions;",
            "import java.util.Map;",
            "import javax.annotation.Generated;",
            "import javax.inject.Provider;",
            "",
            GENERATED_ANNOTATION,
            "public final class DaggerTestComponent implements TestComponent {",
            "  private Provider<Handler> provideAdminHandlerProvider;",
            "  private Provider<Handler> provideLoginHandlerProvider;",
            "  private Provider<Map<WrappedClassKey, Provider<Handler>>>",
            "      mapOfWrappedClassKeyAndProviderOfHandlerProvider;",
            "",
            "  private DaggerTestComponent(Builder builder) {",
            "    initialize(builder);",
            "  }",
            "",
            "  public static Builder builder() {",
            "    return new Builder();",
            "  }",
            "",
            "  public static TestComponent create() {",
            "    return new Builder().build();",
            "  }",
            "",
            "  @SuppressWarnings(\"unchecked\")",
            "  private void initialize(final Builder builder) {",
            "    this.provideAdminHandlerProvider =",
            "        MapModuleOne_ProvideAdminHandlerFactory.create(builder.mapModuleOne);",
            "    this.provideLoginHandlerProvider =",
            "        MapModuleTwo_ProvideLoginHandlerFactory.create(builder.mapModuleTwo);",
            "    this.mapOfWrappedClassKeyAndProviderOfHandlerProvider =",
            "        MapProviderFactory.<WrappedClassKey, Handler>builder(2)",
            "            .put(WrappedClassKeyCreator.createWrappedClassKey(Integer.class),",
            "                provideAdminHandlerProvider)",
            "            .put(WrappedClassKeyCreator.createWrappedClassKey(Long.class),",
            "                provideLoginHandlerProvider)",
            "            .build();",
            "  }",
            "",
            "  @Override",
            "  public Provider<Map<WrappedClassKey, Provider<Handler>>> dispatcher() {",
            "    return mapOfWrappedClassKeyAndProviderOfHandlerProvider;",
            "  }",
            "",
            "  public static final class Builder {",
            "    private MapModuleOne mapModuleOne;",
            "    private MapModuleTwo mapModuleTwo;",
            "",
            "    private Builder() {",
            "    }",
            "",
            "    public TestComponent build() {",
            "      if (mapModuleOne == null) {",
            "        this.mapModuleOne = new MapModuleOne();",
            "      }",
            "      if (mapModuleTwo == null) {",
            "        this.mapModuleTwo = new MapModuleTwo();",
            "      }",
            "      return new DaggerTestComponent(this);",
            "    }",
            "",
            "    public Builder mapModuleOne(MapModuleOne mapModuleOne) {",
            "      this.mapModuleOne = Preconditions.checkNotNull(mapModuleOne);",
            "      return this;",
            "    }",
            "",
            "    public Builder mapModuleTwo(MapModuleTwo mapModuleTwo) {",
            "      this.mapModuleTwo = Preconditions.checkNotNull(mapModuleTwo);",
            "      return this;",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources())
        .that(
            ImmutableList.of(
                mapModuleOneFile,
                mapModuleTwoFile,
                wrappedClassKeyFile,
                HandlerFile,
                LoginHandlerFile,
                AdminHandlerFile,
                componentFile))
        .withCompilerOptions(compilerMode.javacopts())
        .processedWith(new ComponentProcessor(), new AutoAnnotationProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(generatedComponent);
  }

  @Test
  public void mapBindingsWithNonProviderValue() {
    JavaFileObject mapModuleOneFile = JavaFileObjects.forSourceLines("test.MapModuleOne",
        "package test;",
        "",
        "import dagger.Module;",
        "import dagger.Provides;",
        "import dagger.multibindings.IntoMap;",
        "",
        "@Module",
        "final class MapModuleOne {",
        "  @Provides @IntoMap @PathKey(PathEnum.ADMIN) Handler provideAdminHandler() {",
        "    return new AdminHandler();",
        "  }",
        "}");
    JavaFileObject mapModuleTwoFile = JavaFileObjects.forSourceLines("test.MapModuleTwo",
        "package test;",
        "",
        "import dagger.Module;",
        "import dagger.Provides;",
        "import dagger.multibindings.IntoMap;",
        "",
        "@Module",
        "final class MapModuleTwo {",
        "  @Provides @IntoMap @PathKey(PathEnum.LOGIN) Handler provideLoginHandler() {",
        "    return new LoginHandler();",
        "  }",
        "}");
    JavaFileObject enumKeyFile = JavaFileObjects.forSourceLines("test.PathKey",
        "package test;",
        "import dagger.MapKey;",
        "import java.lang.annotation.Retention;",
        "import static java.lang.annotation.RetentionPolicy.RUNTIME;",
        "",
        "@MapKey(unwrapValue = true)",
        "@Retention(RUNTIME)",
        "public @interface PathKey {",
        "  PathEnum value();",
        "}");
    JavaFileObject pathEnumFile = JavaFileObjects.forSourceLines("test.PathEnum",
        "package test;",
        "",
        "public enum PathEnum {",
        "    ADMIN,",
        "    LOGIN;",
        "}");
    JavaFileObject HandlerFile = JavaFileObjects.forSourceLines("test.Handler",
        "package test;",
        "",
        "interface Handler {}");
    JavaFileObject LoginHandlerFile = JavaFileObjects.forSourceLines("test.LoginHandler",
        "package test;",
        "",
        "class LoginHandler implements Handler {",
        "  public LoginHandler() {}",
        "}");
    JavaFileObject AdminHandlerFile = JavaFileObjects.forSourceLines("test.AdminHandler",
        "package test;",
        "",
        "class AdminHandler implements Handler {",
        "  public AdminHandler() {}",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.TestComponent",
        "package test;",
        "",
        "import dagger.Component;",
        "import java.util.Map;",
        "import javax.inject.Provider;",
        "",
        "@Component(modules = {MapModuleOne.class, MapModuleTwo.class})",
        "interface TestComponent {",
        "  Provider<Map<PathEnum, Handler>> dispatcher();",
        "}");
    JavaFileObject generatedComponent =
        JavaFileObjects.forSourceLines(
            "test.DaggerTestComponent",
            "package test;",
            "",
            "import dagger.internal.MapFactory;",
            "import dagger.internal.Preconditions;",
            "import java.util.Map;",
            "import javax.annotation.Generated;",
            "import javax.inject.Provider;",
            "",
            GENERATED_ANNOTATION,
            "public final class DaggerTestComponent implements TestComponent {",
            "  private Provider<Handler> provideAdminHandlerProvider;",
            "  private Provider<Handler> provideLoginHandlerProvider;",
            "  private Provider<Map<PathEnum, Handler>> mapOfPathEnumAndHandlerProvider;",
            "",
            "  private DaggerTestComponent(Builder builder) {",
            "    initialize(builder);",
            "  }",
            "",
            "  public static Builder builder() {",
            "    return new Builder();",
            "  }",
            "",
            "  public static TestComponent create() {",
            "    return new Builder().build();",
            "  }",
            "",
            "  @SuppressWarnings(\"unchecked\")",
            "  private void initialize(final Builder builder) {",
            "    this.provideAdminHandlerProvider =",
            "        MapModuleOne_ProvideAdminHandlerFactory.create(builder.mapModuleOne);",
            "    this.provideLoginHandlerProvider =",
            "        MapModuleTwo_ProvideLoginHandlerFactory.create(builder.mapModuleTwo);",
            "    this.mapOfPathEnumAndHandlerProvider =",
            "        MapFactory.<PathEnum, Handler>builder(2)",
            "            .put(PathEnum.ADMIN, provideAdminHandlerProvider)",
            "            .put(PathEnum.LOGIN, provideLoginHandlerProvider)",
            "            .build();",
            "  }",
            "",
            "  @Override",
            "  public Provider<Map<PathEnum, Handler>> dispatcher() {",
            "    return mapOfPathEnumAndHandlerProvider;",
            "  }",
            "",
            "  public static final class Builder {",
            "    private MapModuleOne mapModuleOne;",
            "    private MapModuleTwo mapModuleTwo;",
            "",
            "    private Builder() {",
            "    }",
            "",
            "    public TestComponent build() {",
            "      if (mapModuleOne == null) {",
            "        this.mapModuleOne = new MapModuleOne();",
            "      }",
            "      if (mapModuleTwo == null) {",
            "        this.mapModuleTwo = new MapModuleTwo();",
            "      }",
            "      return new DaggerTestComponent(this);",
            "    }",
            "",
            "    public Builder mapModuleOne(MapModuleOne mapModuleOne) {",
            "      this.mapModuleOne = Preconditions.checkNotNull(mapModuleOne);",
            "      return this;",
            "    }",
            "",
            "    public Builder mapModuleTwo(MapModuleTwo mapModuleTwo) {",
            "      this.mapModuleTwo = Preconditions.checkNotNull(mapModuleTwo);",
            "      return this;",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources())
        .that(
            ImmutableList.of(
                mapModuleOneFile,
                mapModuleTwoFile,
                enumKeyFile,
                pathEnumFile,
                HandlerFile,
                LoginHandlerFile,
                AdminHandlerFile,
                componentFile))
        .withCompilerOptions(compilerMode.javacopts())
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(generatedComponent);
  }

  @Test
  public void injectMapWithoutMapBinding() {
    JavaFileObject mapModuleFile = JavaFileObjects.forSourceLines("test.MapModule",
        "package test;",
        "",
        "import dagger.Module;",
        "import dagger.Provides;",
        "import java.util.HashMap;",
        "import java.util.Map;",
        "",
        "@Module",
        "final class MapModule {",
        "  @Provides Map<String, String> provideAMap() {",
        "    Map<String, String> map = new HashMap<String, String>();",
        "    map.put(\"Hello\", \"World\");",
        "    return map;",
        "  }",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.TestComponent",
        "package test;",
        "",
        "import dagger.Component;",
        "import java.util.Map;",
        "",
        "@Component(modules = {MapModule.class})",
        "interface TestComponent {",
        "  Map<String, String> dispatcher();",
        "}");
    JavaFileObject generatedComponent;
    switch (compilerMode) {
      case EXPERIMENTAL_ANDROID:
        generatedComponent =
            JavaFileObjects.forSourceLines(
                "test.DaggerTestComponent",
                "package test;",
                "",
                "import dagger.internal.Preconditions;",
                "import java.util.Map;",
                "import javax.annotation.Generated;",
                "",
                GENERATED_ANNOTATION,
                "public final class DaggerTestComponent implements TestComponent {",
                "  private MapModule mapModule;",
                "",
                "  private DaggerTestComponent(Builder builder) {",
                "    initialize(builder);",
                "  }",
                "",
                "  public static Builder builder() {",
                "    return new Builder();",
                "  }",
                "",
                "  public static TestComponent create() {",
                "    return new Builder().build();",
                "  }",
                "",
                "  private Map<String, String> getMapOfStringAndStringInstance() {",
                "    return Preconditions.checkNotNull(",
                "        mapModule.provideAMap(), " + NPE_FROM_PROVIDES_METHOD + ");",
                "  }",
                "",
                "  @SuppressWarnings(\"unchecked\")",
                "  private void initialize(final Builder builder) {",
                "    this.mapModule = builder.mapModule;",
                "  }",
                "",
                "  @Override",
                "  public Map<String, String> dispatcher() {",
                "    return getMapOfStringAndStringInstance();",
                "  }",
                "",
                "  public static final class Builder {",
                "    private MapModule mapModule;",
                "",
                "    private Builder() {}",
                "",
                "    public TestComponent build() {",
                "      if (mapModule == null) {",
                "        this.mapModule = new MapModule();",
                "      }",
                "      return new DaggerTestComponent(this);",
                "    }",
                "",
                "    public Builder mapModule(MapModule mapModule) {",
                "      this.mapModule = Preconditions.checkNotNull(mapModule);",
                "      return this;",
                "    }",
                "  }",
                "}");
        break;
      default:
        generatedComponent =
            JavaFileObjects.forSourceLines(
                "test.DaggerTestComponent",
                "package test;",
                "",
                "import dagger.internal.Preconditions;",
                "import java.util.Map;",
                "import javax.annotation.Generated;",
                "",
                GENERATED_ANNOTATION,
                "public final class DaggerTestComponent implements TestComponent {",
                "  private MapModule mapModule;",
                "",
                "  private DaggerTestComponent(Builder builder) {",
                "    initialize(builder);",
                "  }",
                "",
                "  public static Builder builder() {",
                "    return new Builder();",
                "  }",
                "",
                "  public static TestComponent create() {",
                "    return new Builder().build();",
                "  }",
                "",
                "  @SuppressWarnings(\"unchecked\")",
                "  private void initialize(final Builder builder) {",
                "    this.mapModule = builder.mapModule;",
                "  }",
                "",
                "  @Override",
                "  public Map<String, String> dispatcher() {",
                "    return Preconditions.checkNotNull(",
                "        mapModule.provideAMap(), " + NPE_FROM_PROVIDES_METHOD + ");",
                "  }",
                "",
                "  public static final class Builder {",
                "    private MapModule mapModule;",
                "",
                "    private Builder() {",
                "    }",
                "",
                "    public TestComponent build() {",
                "      if (mapModule == null) {",
                "        this.mapModule = new MapModule();",
                "      }",
                "      return new DaggerTestComponent(this);",
                "    }",
                "",
                "    public Builder mapModule(MapModule mapModule) {",
                "      this.mapModule = Preconditions.checkNotNull(mapModule);",
                "      return this;",
                "    }",
                "  }",
                "}");
    }
    assertAbout(javaSources())
        .that(ImmutableList.of(mapModuleFile, componentFile))
        .withCompilerOptions(compilerMode.javacopts())
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(generatedComponent);
  }

  @Test
  public void mapBindingsWithDuplicateKeys() {
    JavaFileObject module =
        JavaFileObjects.forSourceLines(
            "test.MapModule",
            "package test;",
            "",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import dagger.multibindings.StringKey;",
            "import dagger.multibindings.IntoMap;",
            "",
            "@Module",
            "final class MapModule {",
            "  @Provides @IntoMap @StringKey(\"AKey\") Object provideObjectForAKey() {",
            "    return \"one\";",
            "  }",
            "",
            "  @Provides @IntoMap @StringKey(\"AKey\") Object provideObjectForAKeyAgain() {",
            "    return \"one again\";",
            "  }",
            "}");
    JavaFileObject componentFile =
        JavaFileObjects.forSourceLines(
            "test.TestComponent",
            "package test;",
            "",
            "import dagger.Component;",
            "import java.util.Map;",
            "import javax.inject.Provider;",
            "",
            "@Component(modules = {MapModule.class})",
            "interface TestComponent {",
            "  Map<String, Object> objects();",
            "}");
    assertAbout(javaSources())
        .that(ImmutableList.of(module, componentFile))
        .withCompilerOptions(compilerMode.javacopts())
        .processedWith(new ComponentProcessor())
        .failsToCompile()
        .withErrorContaining("The same map key is bound more than once")
        .and()
        .withErrorContaining("provideObjectForAKey()")
        .and()
        .withErrorContaining("provideObjectForAKeyAgain()")
        .and()
        .withErrorCount(1);
  }

  @Test
  public void mapBindingsWithInconsistentKeyAnnotations() {
    JavaFileObject module =
        JavaFileObjects.forSourceLines(
            "test.MapModule",
            "package test;",
            "",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import dagger.multibindings.StringKey;",
            "import dagger.multibindings.IntoMap;",
            "",
            "@Module",
            "final class MapModule {",
            "  @Provides @IntoMap @StringKey(\"AKey\") Object provideObjectForAKey() {",
            "    return \"one\";",
            "  }",
            "",
            "  @Provides @IntoMap @StringKeyTwo(\"BKey\") Object provideObjectForBKey() {",
            "    return \"two\";",
            "  }",
            "}");
    JavaFileObject stringKeyTwoFile =
        JavaFileObjects.forSourceLines(
            "test.StringKeyTwo",
            "package test;",
            "",
            "import dagger.MapKey;",
            "",
            "@MapKey(unwrapValue = true)",
            "public @interface StringKeyTwo {",
            "  String value();",
            "}");
    JavaFileObject componentFile =
        JavaFileObjects.forSourceLines(
            "test.TestComponent",
            "package test;",
            "",
            "import dagger.Component;",
            "import java.util.Map;",
            "",
            "@Component(modules = {MapModule.class})",
            "interface TestComponent {",
            "  Map<String, Object> objects();",
            "}");
    assertAbout(javaSources())
        .that(ImmutableList.of(module, stringKeyTwoFile, componentFile))
        .withCompilerOptions(compilerMode.javacopts())
        .processedWith(new ComponentProcessor())
        .failsToCompile()
        .withErrorContaining("uses more than one @MapKey annotation type")
        .and()
        .withErrorContaining("provideObjectForAKey()")
        .and()
        .withErrorContaining("provideObjectForBKey()")
        .and()
        .withErrorCount(1);
  }
}
