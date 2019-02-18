import * as React from 'react'
import * as PopoutView from '../../../component/dropdown/popout/dropdown.popout.view'
import * as ExportActionsView from '../../../component/export-actions/export-actions.view'
import MarionetteRegionContainer from '../marionette-region-container'
import { Model } from '.'
import { hot } from 'react-hot-loader'

const ExportActions = (props: any) => {
  return (
    <MarionetteRegionContainer
      data-help="Opens the available actions for the item."
      className="metacard-interaction interaction-actions-export composed-menu"
      view={createResultActionsExportRegion(props.model)}
      viewOptions={{ model: props.model }}
    />
  )
}

const createResultActionsExportRegion = (model: Model) =>
  PopoutView.createSimpleDropdown({
    componentToShow: ExportActionsView,
    dropdownCompanionBehaviors: {
      navigation: {},
    },
    modelForComponent: model.first(),
    leftIcon: 'fa fa-external-link',
    rightIcon: 'fa fa-chevron-down',
    label: 'Export as',
  })

export default hot(module)(ExportActions)
