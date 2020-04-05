package com.leo.buildsrc

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class GodPlugin extends Transform implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println("---------god plugin start---------")
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
        println("---------god plugin  end  ---------")
    }

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
                            handleFile(it)
                        }
                    }
                }
                def dest = transformInvocation.outputProvider.getContentLocation(it.name,
                        it.contentTypes, it.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(it.file, dest)
            }
            //对类型为jar文件的input进行遍历
            it.jarInputs.each {JarInput jarInput->

                //jar文件一般是第三方依赖库jar文件

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

    @Override
    String getName() {
        return "GodPlugin"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }
}