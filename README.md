![27064948-879F-4B9B-90AE-23626CA5C82A](/var/folders/tf/sh8cq6fn56l8pyqj27v2kh7m0000gn/T/com.yinxiang.Mac/com.yinxiang.Mac/WebKitDnD.TB66Xn/27064948-879F-4B9B-90AE-23626CA5C82A.png)

**1.背景**

   android app打包流程大致如上图，dex命令会将第三方库和自身源码转化为dex文件，因此在上图红框过程中可以获取class进行进行字节码增强技术，实现某些需求，比如统计方法执行时长，动态代理等aop操作

**1.1 dex与class区别**

- dvm虚拟机执行的是dex格式文件，jvm执行class文件
- dvm是基于寄存器的虚拟机，jvm是基于虚拟栈的虚拟机，寄存器存取速度比栈快，dvm可以根据硬件设备优化，比较适合移动设备，但是基于
- class文件存在冗余信息，dex工具会去除冗余信息，提升了I/O速度

**2.android plugin开发**

   gradle插件：https://docs.gradle.org/current/userguide/custom_plugins.html

**2.1 自定义plugin的三种方式（java、groovy、kotlin）**

  a.直接在module的build.gradle开发

  b.新建buildSrc module，优点是不需要配置

  c.创建通用plugin，发不到maven

```
# 方式1，modul->build.gradle
apply plugin: 'com.android.application'
apply plugin: HelloPlugin
class HelloPlugin implements Plugin<Project>{
    @Override
    void apply(Project project) {
        project.task('testPlugin') << {
            println 'hello, plugin!'
        }
    }
}
```

**3.asm demo 统计方法耗时**

a.新建plugin，通过第二种buildSrc方式

```
# build.grdle
apply plugin: 'groovy'
apply plugin: 'maven'
dependencies {
    implementation gradleApi()
    implementation localGroovy()
    //noinspection GradleDependency
    implementation 'com.android.tools.build:gradle:3.5.1'
}
repositories {
    google()
    jcenter()
    mavenCentral()
}
```

b. groovy plugin

```
// 1.实现plugin接口，主要有apply方法，插件方法
//   继承Transorm，对class文件进行处理
class GodPlugin extends Transform implements Plugin<Project> {
    // 2. plugin入口方法
    @Override
    void apply(Project project) {
        println("---------god plugin start————")
        // 该插件注册Transform
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
        println("---------god plugin  end  ---------")
    }

    // 3. 逻辑实现入口
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        transformInvocation.inputs.each {
            // directory
            it.directoryInputs.each {
                if (it.file.isDirectory()) {
                    it.file.eachFileRecurse {
                        def fileName = it.name
                        if (fileName.endsWith(".class") &&
                                !fileName.startsWith("R\$") &&
                                fileName != "BuildConfig.class" &&
                                fileName != "R.class") {
                            // 逻辑实现
                            handleFile(it)
                        }
                    }
                }
                // 处理完输入文件之后，要把输出给下一个任务
                def dest = transformInvocation.outputProvider.getContentLocation(it.name,
                        it.contentTypes, it.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(it.file, dest)
            }
            //对类型为jar文件的input进行遍历
            it.jarInputs.each {JarInput jarInput->
                //jar文件一般是第三方依赖库jar文≈
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if(jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0,jarName.length()-4)
                }
                //生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(jarName+md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                //将输入内容复制到输出
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
    }
    // 字节码插入逻辑
    private static void handleFile(File file) {
        def cr = new ClassReader(file.bytes)
        def cw = new ClassWriter(cr,ClassWriter.COMPUTE_MAXS)
        def classVisitor=new MethodTime(Opcodes.ASM6, cw)
        cr.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        def bytes=cw.toByteArray()
        FileOutputStream fos=new FileOutputStream(file.getParentFile().getAbsolutePath()+File.separator+file.name)
        println(file.getParentFile().getAbsolutePath()+File.separator+file.name)
        fos.write(bytes)
        fos.close()
    }
    // 4.该Transfrom的Task名称
    @Override
    String getName() {
        return "GodPlugin"
    }
    // 5.该Transorm要处理的数据类型，CLASSES代表字节码，包括jar，RESOURCES表示标准java资源
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }
    // 6.该Transform作用范围，比如PROJECT表示处理当前项目，PROJECT_LOCAL_DEPS表示本地依jar等
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }
}
```

c. asm插入，通过idea plugin

  通过ASM Bytecode插件的变量命名是默认的0，1，需要手动改为比较大的来避免与要插入方法的变量名冲突

```
public class MethodTime extends ClassVisitor {
    public MethodTime(int api, ClassVisitor cv) {
        super(api, cv);
    }
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        methodVisitor = new AdviceAdapter(Opcodes.ASM6, methodVisitor, access, name, desc) {
            boolean inject = true;
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                // 通过注解判断是否需要asm处理
                inject = false;
                return super.visitAnnotation(desc, visible);
            }

            @Override
            protected void onMethodEnter() {
                if (!inject) {
                    return;
                }
                ...
            }

            @Override
            protected void onMethodExit(int opcode) {
                if (!inject) {
                    return;
                }
            }
        };
        return methodVisitor;
    }
}

```

