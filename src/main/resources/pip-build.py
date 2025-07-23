#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
描述：检查 pip 缓存以确定依赖项层次结构的脚本
用法： pip-build.py --projectname=<project_name> --requirements=<requirements_path>
"""
import argparse
import sys
from os import path
from re import split
from typing import Callable, Type, Optional, Set, List, Tuple
from pkg_resources import working_set, Requirement, Distribution
from pkg_resources import RequirementParseError
import pip


class PipBuildResultNode:
    """在树形图中表示Python依赖项，包含名称、版本和子依赖项"""

    def __init__(self, package_name: str, package_version: str):
        self.package_name = package_name  # 依赖项名称
        self.package_version = package_version  # 依赖项版本
        self.dependencies: List[PipBuildResultNode] = []  # 子依赖项列表

    def build_result(self, indent_level: int = 1) -> str:
        """构建要打印到命令行的依赖项树字符串"""
        result = [f"{self.package_name}=={self.package_version}"]
        indent = " " * 4 * indent_level

        for dependency in self.dependencies:
            result.append(f"{indent}{dependency.build_result(indent_level + 1)}")

        return "\n".join(result)


class PipBuild:
    def __init__(self):
        # 初始化pip相关组件
        self._parse_requirements, self._pip_session_cls = self._get_pip_components()

    @classmethod
    def _get_pip_components(cls) -> Tuple[Callable, Type]:
        """根据pip版本返回对应的解析器和会话类"""
        try:
            # 提取主版本号，处理可能的非数字前缀（如'b'表示beta版）
            version_str = pip.__version__
            major_version = int(''.join(filter(str.isdigit, version_str.split('.')[0])))
        except (IndexError, ValueError):
            sys.stderr.write(f"无法解析pip版本: {pip.__version__}")
            sys.exit(1)

        try:
            if major_version < 10:
                from pip.req import parse_requirements
                from pip.download import PipSession
            elif major_version < 20:
                from pip._internal.req import parse_requirements
                from pip._internal.download import PipSession
            else:
                from pip._internal.req import parse_requirements
                from pip._internal.network.session import PipSession

            return parse_requirements, PipSession
        except ImportError as e:
            sys.stderr.write(f"无法导入pip模块: {e}")
            sys.exit(1)

    @classmethod
    def parse_command_line_args(cls) -> argparse.Namespace:
        """解析命令行参数并返回命名空间对象"""
        parser = argparse.ArgumentParser(description='检查pip缓存中的依赖项层次结构')
        parser.add_argument('-p', '--projectname', required=True,
                            help='项目名称（短选项-p，长选项--projectname）')
        parser.add_argument('-r', '--requirements', required=True,
                            help='依赖文件路径（短选项-r，长选项--requirements）')

        return parser.parse_args()

    def handle_requirements_file(self, requirements_path: str, root_node: PipBuildResultNode) -> None:
        """处理requirements文件：检查存在性并填充依赖树"""
        if not path.exists(requirements_path):
            sys.stderr.write(f"错误：依赖文件不存在 - {requirements_path}\n")
            sys.exit(1)

        if not path.isfile(requirements_path):
            sys.stderr.write(f"错误：不是有效的文件 - {requirements_path}\n")
            sys.exit(1)

        self._fill_dependency_tree(root_node, requirements_path)

    def _fill_dependency_tree(self, root_node: PipBuildResultNode, requirements_path: str) -> None:
        """解析requirements文件并填充依赖树"""
        try:
            session = self._pip_session_cls()
            parsed_requirements = self._parse_requirements(requirements_path, session=session)

            for req in parsed_requirements:
                # 可靠地获取包名
                if hasattr(req, 'req') and req.req:
                    package_name = req.req.name
                else:
                    try:
                        package_name = Requirement.parse(req.requirement).name
                    except RequirementParseError:
                        # 作为最后的手段，使用字符串分割
                        package_name = split('===|<=|!=|==|>=|~=|<|>', req.requirement)[0].strip()

                # 递归解析依赖
                dependency_node = self._recursively_parse_dependencies(package_name)
                if dependency_node:
                    root_node.dependencies.append(dependency_node)
                else:
                    sys.stderr.write(f"警告：无法解析依赖 - {package_name}\n")

        except Exception as e:
            sys.stderr.write(f"处理依赖文件时出错 {requirements_path}: {str(e)}\n")
            sys.exit(1)

    def parse_project_node(self, project_name: str) -> PipBuildResultNode:
        """解析项目根节点依赖"""
        if project_name:
            dependency_node = self._recursively_parse_dependencies(project_name)
            if dependency_node:
                return dependency_node

        # 返回默认节点表示无法解析
        return PipBuildResultNode('n?', 'v?')

    def _recursively_parse_dependencies(self, package_name: str, visited_packages: Optional[Set[str]] = None) -> \
    Optional[PipBuildResultNode]:
        """递归解析依赖项及其子依赖"""
        visited = visited_packages if visited_packages is not None else set()

        # 获取包信息
        package = self._get_package_by_name(package_name)
        if not package:
            return None

        # 创建当前依赖节点
        node = PipBuildResultNode(package.project_name, package.version)

        # 避免循环依赖
        normalized_name = package_name.lower()
        if normalized_name not in visited:
            visited.add(normalized_name)

            # 递归解析子依赖
            for dep in package.requires():
                child_node = self._recursively_parse_dependencies(dep.key, visited)
                if child_node:
                    node.dependencies.append(child_node)

        return node

    @classmethod
    def _get_package_by_name(cls, package_name: str) -> Optional[Distribution]:
        """从pip缓存中查找包，支持多种命名变体"""
        if not package_name:
            return None

        package_dict = working_set.by_key

        # 尝试直接解析
        try:
            req = Requirement.parse(package_name)
            if req.key in package_dict:
                return package_dict[req.key]
        except RequirementParseError:
            pass

        # 尝试常见的包名变体（处理横杠、下划线、点的差异）
        name_variants = {
            package_name,
            package_name.lower(),
            package_name.replace('-', '_'),
            package_name.replace('_', '-'),
            package_name.replace("-", "."),
            package_name.replace(".", "-")
        }

        for variant in name_variants:
            if variant in package_dict:
                return package_dict[variant]

        return None

    def main(self) -> None:
        """主函数：协调参数解析、依赖处理和结果输出"""
        args = self.parse_command_line_args()

        # 构建项目依赖根节点
        project_node = self.parse_project_node(args.projectname)

        # 处理依赖文件
        self.handle_requirements_file(args.requirements, project_node)

        # 输出结果
        print(project_node.build_result())


if __name__ == '__main__':
    pip_build = PipBuild()
    pip_build.main()
