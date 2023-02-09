package org.devops

//格式化输出
def PrintMes(value,color){
    colors = ['red'   : "\033[40;31m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m",
              'blue'  : "\033[47;34m ${value} \033[0m",
              'green' : "[1;32m>>>>>>>>>>${value}>>>>>>>>>>[m",
              'green1' : "\033[40;32m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m" ]
    ansiColor('xterm') {
        println(colors[color])
    }
}

//获取时间
def GetTime(pattern){
    patterns = pattern ? pattern : "yyyy-MM-dd HH:mm:ss"
    return new Date().format(patterns)
}

//执行scan,nodejs容器提供环境
def SonarScanWithContainer(sonarServer,sonarqube_properties,branchName,nodejs_tools){
    // 为了确保前端项目扫描缺少nodejs依赖，这里默认提供nodejs环境
    nodejs("${nodejs_tools}"){
        //定义sonar服务器
        withSonarQubeEnv("${sonarServer}"){
            def scannerHome = tool "sonar-scanner"
            println "使用内置默认参数配置"
            writeFile file: 'sonar-project.properties',
                    text: """sonar.projectKey=${JOB_BASE_NAME}\n"""+
                            """sonar.projectName=${JOB_BASE_NAME}\n"""+
                            """sonar.projectVersion=${BUILD_NUMBER}\n"""+
                            """sonar.branch.name=${branchName}\n"""+
                            """${sonarqube_properties}\n"""+
                            """sonarqube_properties\n"""
            sh "cat sonar-project.properties"
            sh "${sonarqubeScanner}/bin/sonar-scanner"
        }

    }
}

//执行scan
def SonarScanWith(sonarServer,sonarqube_properties,branchName){
    //定义sonar服务器
    withSonarQubeEnv("${sonarServer}"){
        def scannerHome = tool "sonar-scanner"
        println "使用内置默认参数配置"
        writeFile file: 'sonar-project.properties',
                text: """sonar.projectKey=${JOB_BASE_NAME}\n"""+
                        """sonar.projectName=${JOB_BASE_NAME}\n"""+
                        """sonar.projectVersion=${BUILD_NUMBER}\n"""+
                        """sonar.branch.name=${branchName}\n"""+
                        """${sonarqube_properties}\n"""+
                        """sonarqube_properties\n"""
        sh "cat sonar-project.properties"
        sh "${sonarqubeScanner}/bin/sonar-scanner"
    }
}

// pom文件读取
def ReadPomFile(pom_path){
    pom_param = pom_path ? pom_path : "./pom.xml"
    println("pom文件路径为：${pom_param}")
    pom = readMavenPom file: "${pom_param}"
    return pom
}

// 获取pom文件中项目的artifactId(项目名称)
def GetPomArtifactId(pom){
    app_name = pom.artifactId
    return app_name
}

// 获取pom文件中项目的版本
def GetPomVersion(pom){
    app_version = pom.version
    return app_version
}

// 构建类型
def Build(buildType,buildShell){
    def buildTools = ["mvn":"M2","ant":"ANT","gradle":"GRADLE","npm":"NPM"]

    println("当前选择的构建类型为${buildType}")
    buildHome = tool buildTools[buildType]

    if("${buildType}"=="npm"){
        sh """
      export NODE_HOME=${buildHome}
      export PATH=\$NODE_HOME/bin:\$PATH
    """
    }else{
        sh "${buildHome}/bin/${buildShell}"
    }
}

// 基于docker容器提供构建环境
def BuildWithContainer(buildType,buildShell){
    def buildTools = ["mvn":"maven-8","ant":"ANT","gradle":"GRADLE","npm":"NPM"]
    println("当前选择的构建类型为${buildType}")
    container(buildTools[buildType]){
        sh "${buildShell}"
    }
}

// 读取文本参数，将内容写到当前目录的文件中
def WriteFile(fileParam,fileName){
    writeFile file: fileName, text: "${fileParam}"
    sh "cat ${fileName}"
}

// 创建镜像tag
def CreateImageTag(app_version,branch){
    tagName = "${app_version}${branch}"
    return tagName
}
// 创建镜像名称
def CreateImageName(docker_hub,app_name,tagName,docker_project){
    imageName = "$docker_hub/$docker_project/${app_name}:${tagName}"
    return imageName
}
// 构建镜像
def BuildImage(credentialId,docker_hub_type,docker_hub,imageName){
    docker.withRegistry("$docker_hub_type://$docker_hub", "$credentialId") {
        println("构建镜像")
        customImage = docker.build(imageName)
        println("推送镜像")
        customImage.push()
        println("删除镜像")
        sh "docker rmi ${imageName}"
    }
}

// 执行Helm的方法
def helmDeploy(Map args) {
    if(args.dry_run){
        println("尝试 Helm 部署，验证是否能正常部署")
    } else {
        println("正式 Helm 部署")
        sh "helm upgrade --install ${args.name} --namespace ${args.namespace} ${args.values} --set ${args.set} ${helm_chart}"
    }
}

// 执行Helm部署
def Helm(kubernetes_secret_id,kubernetes_server,helm_param,docker_hub,docker_project,app_name,app_version,branch,kubernetes_namespace){
    withKubeConfig([credentialsId: "$kubernetes_secret_id",serverUrl: "$kubernetes_server"]) {
        println("生成value.yaml文件")
        WriteFile(helm_param,"values.yaml")
        println("检测是否存在yaml文件")
        def values = ""
        if (fileExists('values.yaml')) {
            values = "-f values.yaml"
        }
        // 设置set参数
        set_param = "image.repository=$docker_hub/$docker_project/${app_name}," +
                "image.tag=${app_version}${branch}," +
                "podAnnotations.update-random=${UUID.randomUUID().toString()}," +
                "sidecar.env[0].name=JOB_BUILD," +
                "sidecar.env[0].value=${JOB_BASE_NAME}-${BUILD_NUMBER}," +
                "sidecar.resources.limits.cpu=50m," +
                "sidecar.resources.limits.memory=20Mi," +
                "ingress.path=/${kubernetes_namespace}/${app_name}," +
                "ingress.service=${app_name}"
        // 执行 Helm 方法
        echo "Helm 执行部署测试"
        helmDeploy(init: false ,dry_run: true ,name: "${app_name}" ,namespace: "${kubernetes_namespace}" ,set: "${set_param}" , values: "${values}" ,template: "spring-boot")
        echo "Helm 正式执行部署"
        helmDeploy(init: false ,dry_run: false ,name: "${app_name}" ,namespace: "${kubernetes_namespace}" ,set: "${set_param}" , values: "${values}" ,template: "spring-boot")
    }
}

// 将文件推送到gitlab
def PushFileToGit(git_credentialsId,push_url,fileName,git_push_username,git_push_email){
    println "输出渲染模板存储到文件"
        git branch: "master" ,changelog: false , credentialsId: "$git_credentialsId", url: "${push_url}"
        if (fileExists("${fileName}")) {
            sh "cat ${fileName}"
            withCredentials([usernamePassword(credentialsId: "$git_credentialsId", passwordVariable: "GIT_PASSWORD", usernameVariable: "GIT_USERNAME")]) {
                sh 'git config --local credential.helper "!p() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; p"'
                sh """
                                            git config --global user.name  "${git_push_username}"
                                            git config --global user.email "${git_push_email}"
                                            git add -f ${fileName}
                                            git commit -m 提交${fileName}文件
                                            git push origin master
                                          """
            }
        }

}

// 拉取代码
def CheckOut(scm_type,url,credentialsId,branchName){
    if (scm_type == "svn"){
        checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[cancelProcessOnExternalsFail: true, credentialsId: "${credentialsId}", depthOption: 'infinity', ignoreExternalsOption: true, local: '.', remote: "${url}"]], quietOperation: true, workspaceUpdater: [$class: 'UpdateUpdater']])

    }else{
        println("git拉取代码")
        git branch: branchName ,changelog: true , credentialsId: credentialsId, url: url
    }
}

// 通过id读取文件并写入当前目录
def WriteFileFromId(maven_settings_id,fileName){
    configFileProvider([configFile(fileId: maven_settings_id, targetLocation: fileName)]) {
        println("创建文件${fileName}")
    }

}