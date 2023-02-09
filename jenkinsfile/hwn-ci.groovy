@Library('hwn-jenkins-lib@master') _
import org.devops.tools
import org.devops.toemail

//func form sharedlibrary
def tools = new org.devops.tools()
def toemail = new org.devops.toemail()

def git_credentialsId = "6ec74867-2832-4015-9ada-25acc2c76b78"
def branch = "main"
def userName = "fengsulin"
def buildType = "mvn"
def buildShell = "mvn -U -B -e clean install -T 1C -Dmaven.test.skip=true --settings settings.xml"
def git_url = "http://git.rdc.i139.cn/tssd-commons-services/netmonitor/cmdb-service"
def runOpts
def maven_settings_id = "76a57f8d-a5f1-4cc0-a235-b447bebefba9"
def hub_credentialId = ""
def docker_hub = ""
def app_name = ""
def app_version = ""
def docker_hub_type = "https"
def docker_project = "net-monitor"
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

        stage('初始化'){
            steps{
                script{
                    tools.PrintMes("初始化","green")
                }
            }
        }
        stage('CheckOut'){
            steps{
                script{
                    println "${branch}"

                    tools.PrintMes("获取代码","green")
                    tools.CheckOut("git",git_url,git_credentialsId,branch)
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
                    tagName = tools.CreateImageTag(app_version,branch)
                    imageName = tools.CreateImageName(docker_hub,app_name,tagName,docker_project)
                    tools.BuildImage(credentialId,docker_hub_type,docker_hub,imageName)
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
