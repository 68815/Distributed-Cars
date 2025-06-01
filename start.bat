@echo off
setlocal

:: 设置 JAR 文件路径
set JAR_PATH="E:\repositess\engineeing\Distributed-Cars\build\libs\Distributed-Cars-0.0.1-SNAPSHOT.jar"

:: 设置要启动的实例数量
set INSTANCE_COUNT=5

:: 循环启动实例
for /L %%i in (1,1,%INSTANCE_COUNT%) do (
    start "Instance %%i" java -jar %JAR_PATH%
)

echo 已启动 %INSTANCE_COUNT% 个实例...
timeout /t 3 >nul