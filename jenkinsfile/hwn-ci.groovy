@Library('hwn-jenkins-lib@master') _
import org.devops.tools
import org.devops.toemail
import org.devops.kubernetes

//func form sharedlibrary
def tools = new org.devops.tools()
def toemail = new org.devops.toemail()
def k8s = new org.devops.kubernetes()

def git_credentialsId = "6ec74867-2832-4015-9ada-25acc2c76b78"
def branch = "main"
def userName = "fengsulin"
def buildType = "mvn"
def buildShell = "mvn -U -B -e clean install -T 1C -Dmaven.test.skip=true --settings settings.xml"
def git_url = "http://git.rdc.i139.cn/tssd-commons-services/netmonitor/cmdb-service"
def runOpts
def maven_settings_id = "76a57f8d-a5f1-4cc0-a235-b447bebefba9"
def hub_credentialId = "docker-hub-rdchub-username-password"
def docker_hub = "hub.hwn.i139.cn"
def app_name = ""
def app_version = ""
def docker_hub_type = "https"
def docker_project = "net-monitor"
def pom_path = ""
def docker_dockerfile = "FROM hub.hwn.i139.cn/rdc-commons/official-openjdk:8u242-jre-sh\n" +
        "VOLUME /tmp\n" +
        "ADD target/*.jar app.jar\n" +
        "RUN sh -c 'touch /app.jar'\n" +
        "ENV JAVA_OPTS=\"-Xmx512M -Xms512M -Xss256k -XX:MaxRAMPercentage=80.0 -Duser.timezone=Asia/Shanghai\"\n" +
        "ENV APP_OPTS=\"\"\n" +
        "ENTRYPOINT [ \"sh\", \"-c\", \"java \$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar \$APP_OPTS\" ]"
//env

//branch
//branchName = branch - "refs/heads/"
currentBuild.description = "Trigger by ${userName} ${branch}"

//判断git触发自动执行还是手动
if("${runOpts}" == "GitlabPush"){
    branchName = branch - "refs/heads"
    currentBuild.description = "Trigger by ${userName} ${branch}"
    env.runOpts = "GitlabPush"
}else{
    userEmail = "fengsu_lin@163.com"
}


//def label = 'jnlp-agent'
pipeline{

    agent{
        label "jnlp-agent-node"
    }
    stages{

        stage('CheckOut'){
            steps{
                script{
                    println "${branch}"

                    tools.PrintMes("获取代码","green")
                    tools.CheckOut("git",git_url,git_credentialsId,branch)
                }
            }
        }
        stage('初始化'){
            steps{
                script{
                    tools.PrintMes("初始化","green")
                    pom = tools.ReadPomFile(pom_path)
                    app_name = tools.GetPomArtifactId(pom)
                    app_version = tools.GetPomVersion(pom)
                    branch = "${branch}" == "master" ? "":"${branch}" == "main" ? "" : "-${branch}"

                }
            }
        }

        stage("Build"){
            steps{
                script{
                    tools.PrintMes("执行打包","green")
                    tools.WriteFileFromId(maven_settings_id,"settings.xml")
                    tools.BuildWithContainer(buildType,buildShell)
                }
            }
        }
        stage("Docker"){
            steps{
                script{
                    tools.PrintMes("镜像构建","green")
                    tools.WriteFile(docker_dockerfile,"Dockerfile")
                    tagName = tools.CreateImageTag(app_version,branch)
                    imageName = tools.CreateImageName(docker_hub,app_name,tagName,docker_project)
                    tools.PrintMes("镜像名称：${imageName}","green")
                    container('docker'){
                        tools.BuildImage(hub_credentialId,docker_hub_type,docker_hub,imageName)
                    }
                }
            }
        }
        stage("k8s"){
            steps{
                script{
                    tools.PrintMes("k8s部署","green")
                    respone = k8s.GetDeployment("dev-tool-kit","monitor-fronted-dist")
                    println("${respone}")
                }
            }
        }


    }

    post{
        always{
            script{
                println("always")
            }
        }
        success{
            script{
                println("success")
                toemail.Email("流水线成功了！",userEmail)

            }
        }
        failure{
            script{
                println("failure")
                toemail.Email("流水线失败了！",userEmail)
            }
        }
        aborted{
            script{
                println("aborted")
                toemail("流水线取消了！",userEmail)
            }
        }
    }
}
