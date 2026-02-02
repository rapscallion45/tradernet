import { Fragment, ReactNode } from "react"

export function wrapNodeList(nodeList?: ReactNode[]) {
  return nodeList?.map((node, nodeIndex) => <Fragment key={nodeIndex}>{node}</Fragment>)
}
