@Library('hwn-jenkins-lib@master') _

//func form sharedlibrary
def tools = new org.devops.tools()
def toemail = new org.devops.toemail()
def credentials_Id = "6ec74867-2832-4015-9ada-25acc2c76b78"
def branch = "main"
def username = "fengsulin"

def runOpts
//env
String buildType = "${env.buildType}".trim()
String buildShell = "${env.buildShell}".trim()
String srcUrl = "${env.srcUrl}".trim()
String branchName = "${env.branchName}".trim()

//branch
//branchName = branch - "refs/heads/"
currentBuild.description = "Trigger by ${userName} {branch}"
gitlab.ChangeCommitStatus(projectId,commitSha,"running")

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
        node{
            kubernetes{
                label:"jnlp-agent-node"
                cloud:"k8s"
            }
        }
    }
    stages{

        stage('CheckOut'){
            steps{
                script{
                    println "${branch}"

                    tools.PrintMes("获取代码","green")
                    tools.
                }
            }
        }
    }
    stage("Build"){
        steps{
            script{
                tools.PrintMes("执行打包","green")
                build.Build(buildType,buildShell)
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
                gitlab.ChangeCommitStatus(projectId,commitSha,"success")
                toemail.Email("流水线成功了！",userEmail)

            }
        }
        failure{
            script{
                println("failure")
                gitlab.ChangeCommitStatus(projectId,commitSha,"failed")
                toemail.Email("流水线失败了！",userEmail)
            }
        }
        aborted{
            script{
                println("aborted")
                gitlab.ChangeCommitStatus(projectId,commitSha,"canceled")
                toemail("流水线取消了！",userEmail)
            }
        }
    }
}
